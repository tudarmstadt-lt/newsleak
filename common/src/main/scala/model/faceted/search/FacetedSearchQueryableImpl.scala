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
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms

// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on

object FacetedSearch extends FacetedSearchQueryableImpl

class FacetedSearchQueryableImpl extends FacetedSearchQueryable {

  private val clientService = new ESTransportClient
  private val elasticSearchIndex = "cable"
  private val keywordsField = "keywords" -> "keywords.keyword.raw"
  private val nodesField = "entities" -> "entities.entId"

  private lazy val aggregationToField =
    aggregationFields().map(k => k -> s"$k.raw").toMap ++ Map(keywordsField, nodesField)

  // Always remove these fields from the aggregation.
  private val defaultExcludedAggregations = List("content")
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

  private def createQuery(fullTextSearch: Option[String], facets: Map[String, List[String]]): BoolQueryBuilder = {
    val request = QueryBuilders.boolQuery()
    // Search for given facets
    facets.map {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.must(QueryBuilders.termQuery(s"$k.raw", meta)))
        request.must(filter)
    }
    // Search for full text string if available
    val fullTextQuery = QueryBuilders.matchQuery("content", fullTextSearch)
    if (fullTextSearch.isDefined) request.must(fullTextQuery) else request
  }

  /**
   * Convert response to our internal model
   */
  private def parseResult(response: SearchResponse, aggregations: Map[String, (String, Int)]): List[Aggregation] = {
    val res = aggregations.collect {
      // Create node bucket for entities
      case (k, (v, s)) if k == nodesField =>
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

  override def searchDocuments(fullTextSearch: Option[String], facets: Map[String, List[String]], pageSize: Int): Iterator[Long] = {
    val requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(fullTextSearch, facets))
      .setSize(pageSize)

    new SearchHitIterator(requestBuilder).map(_.id().toLong)
  }

  override def aggregateAll(
    fullTextSearch: Option[String],
    facets: Map[String, List[String]],
    excludedAggregations: List[String] = List()
  ): List[Aggregation] = {
    val excluded = defaultExcludedAggregations ++ excludedAggregations
    val validAggregations = aggregationToField.filterKeys(!excluded.contains(_))

    aggregate(fullTextSearch, facets, validAggregations.map { case (k, v) => (k, (v, defaultAggregationSize)) })
  }

  override def aggregate(fullTextSearch: Option[String], facets: Map[String, List[String]], aggregationKey: String, size: Int): Option[Aggregation] = {
    val field = aggregationToField(aggregationKey)
    aggregate(fullTextSearch, facets, Map(aggregationKey -> (field, size))).headOption
  }

  private def aggregate(fullTextSearch: Option[String], facets: Map[String, List[String]], aggs: Map[String, (String, Int)]): List[Aggregation] = {
    var requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(fullTextSearch, facets))
      // We are only interested in the document id
      .addFields("id")

    for ((k, (v, size)) <- aggs) {
      val agg = AggregationBuilders.terms(k)
        .field(v)
        .size(size)
      requestBuilder = requestBuilder.addAggregation(agg)
    }

    val response = requestBuilder.execute().actionGet()
    // There is no need to call shutdown, since this node is the only
    // one in the cluster.
    parseResult(response, aggs)
  }
}

/* object Testable extends App {

  val facets = Map(
    "Classification" -> List("CONFIDENTIAL"),
    "Tags" -> List("ASEC", "PREL")
  )

  println(FacetedSearch.aggregateAll(Some("Clinton"), facets, List("Header")))
  println(FacetedSearch.aggregate(Some("Clinton"), facets, "SignedBy", 4))
  val hitIterator = FacetedSearch.searchDocuments(None, facets, 10)
} */ 