/*
 * Copyright (C) 2015  Language Technology Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package model.faceted.search

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders, QueryBuilder}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on

import utils.RichString.richString

object FacetedSearch extends FacetedSearchQueryableImpl

class FacetedSearchQueryableImpl extends FacetedSearchQueryable {

  private val clientService = new ESTransportClient
  private val elasticSearchIndex = "cable"
  // These two fields differ from the generic metadata
  private val keywordsField = "Keywords" -> "Keywords.Keyword.raw"
  private val nodesField = "Entities" -> "Entities.EntId"

  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  private lazy val aggregationToField =
    aggregationFields().map(k => k -> s"$k.raw").toMap ++ Map(keywordsField, nodesField)

  // Always remove these fields from the aggregation.
  private val defaultExcludedAggregations = List("Content")
  private val defaultAggregationSize = 15

  private def aggregationFields(): List[String] = {
    val res = clientService.client.admin().indices().getMappings(new GetMappingsRequest().indices(elasticSearchIndex)).get()
    val mapping = res.mappings().get(elasticSearchIndex)
    val terms = mapping.flatMap { m =>
      val source = m.value.sourceAsMap()
      val properties = source.get("properties").asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, String]]]
      properties.keySet()
    }
    terms.toList
  }

  private def createQuery(facets: Facets): QueryBuilder = {
    if (facets.isEmpty) {
      QueryBuilders.matchAllQuery()
    } else {
      val request = QueryBuilders.boolQuery()
      request
        .must(addFulltextQuery(facets))
        .must(addGenericFilter(facets))
        .must(addEntitiesFilter(facets))
        .must(addDateFilter(facets))

      request
    }
  }

  private def addFulltextQuery(facets: Facets): BoolQueryBuilder = {
    val query = QueryBuilders.boolQuery()
    facets.fullTextSearch.map(t => query.must(QueryBuilders.matchQuery("Content", t)))
    query
  }

  private def addGenericFilter(facets: Facets): BoolQueryBuilder = {
    // Search for generic facets
    val query = QueryBuilders.boolQuery()
    facets.generic.foreach {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.should(QueryBuilders.termQuery(s"$k.raw", meta)))
        query.must(filter)
    }
    query
  }

  private def addEntitiesFilter(facets: Facets): BoolQueryBuilder = {
    val entitiesFilter = QueryBuilders.boolQuery()
    facets.entities.map { e =>
      entitiesFilter.must(QueryBuilders.termQuery(nodesField._2, e))
    }
    entitiesFilter
  }

  private def addDateFilter(facets: Facets): BoolQueryBuilder = {
    val query = QueryBuilders.boolQuery()
    if (facets.fromDate.isDefined || facets.toDate.isDefined) {
      val dateFilter = QueryBuilders
        .rangeQuery("Created")
        .format("yyyy-MM-dd")
        .timeZone("+01:00")

      val gteFilter = facets.fromDate.map(d => dateFilter.gte(d.toString(dateFormat))).getOrElse(dateFilter)
      val lteFilter = facets.toDate.map(d => dateFilter.lte(d.toString(dateFormat))).getOrElse(gteFilter)

      query.must(lteFilter)
    } else {
      query
    }
  }

  /**
   * Convert response to our internal model
   */
  private def parseResult(response: SearchResponse, aggregations: Map[String, (String, Int)]): List[Aggregation] = {
    val res = aggregations.collect {
      // Create node bucket for entities
      case (k, (v, s)) if k == nodesField._1 =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)).toList
        Aggregation(k, buckets)
      case (k, (v, s)) =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
        Aggregation(k, buckets)
    }
    res.toList
  }

  override def searchDocuments(facets: Facets, pageSize: Int): Iterator[Long] = {
    val requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(facets))
      .setSize(pageSize)

    // TODO: We have to figure out, why this returns "4.4.0" with source name kibana as id when we use a matchAllQuery
    new SearchHitIterator(requestBuilder).flatMap(_.id().toLongOpt())
  }

  override def aggregateAll(
    facets: Facets,
    size: Int = defaultAggregationSize,
    excludedAggregations: List[String] = List()
  ): List[Aggregation] = {
    val excluded = defaultExcludedAggregations ++ excludedAggregations
    val validAggregations = aggregationToField.filterKeys(!excluded.contains(_))

    aggregate(facets, validAggregations.map { case (k, v) => (k, (v, size)) }, Nil)
  }

  override def aggregate(facets: Facets, aggregationKey: String, size: Int, filter: List[String]): Aggregation = {
    val field = aggregationToField(aggregationKey)
    aggregate(facets, Map(aggregationKey -> (field, size)), filter).head
  }

  override def aggregateKeywords(facets: Facets, size: Int, filter: List[String]): Aggregation = {
    aggregate(facets, keywordsField._1, size, filter)
  }

  override def aggregateEntities(facets: Facets, size: Int, filter: List[Long]): Aggregation = {
    aggregate(facets, nodesField._1, size, filter.map(_.toString))
  }

  private def aggregate(facets: Facets, aggs: Map[String, (String, Int)], filter: List[String]): List[Aggregation] = {
    var requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(facets))
      // We are only interested in the document id
      .addFields("id")

    for ((k, (v, size)) <- aggs) {
      val agg = AggregationBuilders.terms(k)
        .field(v)
        .size(size)
        .minDocCount(0)

      // Apply filter to the aggregation request
      val filteredAgg = if (filter.isEmpty) agg else agg.include(filter.toArray)
      requestBuilder = requestBuilder.addAggregation(filteredAgg)
    }

    val response = requestBuilder.execute().actionGet()
    // There is no need to call shutdown, since this node is the only
    // one in the cluster.
    parseResult(response, aggs)
  }
}

/* object Testable extends App {

  val genericSimple = Map(
    "Classification" -> List("CONFIDENTIAL")
  )

  val genericComplex = Map(
    "Classification" -> List("CONFIDENTIAL", "UNCLASSIFIED"),
    "Tags" -> List("ASEC", "PREL")
  )

  // Any format (e.g yyyy, yyyy-MM-dd, ...) of LocalDateTime is supported
  val from = LocalDateTime.parse("1966-12-28", DateTimeFormat.forPattern("yyyy-MM-dd"))
  val to = LocalDateTime.parse("1966-12-28", DateTimeFormat.forPattern("yyyy-MM-dd"))

  val emptyFacets = Facets(None, Map(), List(), None, None)
  val dateRangeFacets = Facets(None, Map(), List(), Some(from), Some(to))
  val entityFacets = Facets(None, genericSimple, List(999999), None, None)
  val complexFacets = Facets(None, genericComplex, List(), None, None)


  //println(FacetedSearch.aggregateAll(dateRangeFacets, 10, List("Header")))
  println(FacetedSearch.aggregate(entityFacets, "Tags", 4, List("PREL")))
  //println(FacetedSearch.aggregate(emptyFacets, "Tags", 4))
  // println(FacetedSearch.aggregateKeywords(f, 4))
  // val hitIterator = FacetedSearch.searchDocuments(emptyFacets, 21)

  //val hitIterator = FacetedSearch.searchDocuments(dateRangeFacets, 21)
  //hitIterator.foreach(d => println(d))
} */ 