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

// TODO: parameter size per aggregation
// TODO: Provide method that returns every field e.g classification, with its instances?
// Is this directly possible from ES. Then we don't need metadata in ES anymore.
object FacetedSearch {

  private val clientService = new ESTransportClient
  private val elasticSearchIndex = "cable"
  private val nodesField = "entities"

  private lazy val aggregationToField = Map(
    nodesField -> "entities.entId"
  ) ++ aggregationFields().map(k => k->s"$k.raw")

  // Always remove these fields from the aggregation.
  private val defaultExcludedAggregations = List("content")
  private val defaultTermSize = 10

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

  private def createQuery(facets: Map[String, List[String]]): BoolQueryBuilder = {
    val request = QueryBuilders.boolQuery()
    facets.map {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.must(QueryBuilders.termQuery(s"$k.raw", meta)))
        request.must(filter)
    }
    request
  }

  /**
   * Convert response to our internal model
   */
  private def parseResult(response: SearchResponse, aggregations: Map[String, String]): List[Aggregation] = {
    val res = aggregations.collect {
      // Create node bucket for entities
      case (k, v) if k == nodesField =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)).toList
        Aggregation(k, buckets)
      case (k, v) =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
        Aggregation(k, buckets)
    }
    res.toList
  }

  /**
   * Applies the given facets to the underling document collection and returns aggregations
   * for all available metadata. The response also contains an aggregation for prominent
   * entities.
   *
   * @param facets maps from metadata term keys to a list of possible instances for the given
   *               term key. Multiple instances as values and facets will be joined via 'and'.
   *
   *               The following example will query for documents that are 'CONFIDENTIAL' and
   *               tagged with 'ASEC' as well as 'PREL'.
   *
   *               val facets = Map(
   *                 "Classification" -> List("CONFIDENTIAL"),
   *                  "Tags" -> List("ASEC", "PREL")
   *               )
   *               FacetedSearch.search(facets)
   * @param excludedAggregations given metadata keys will be excluded from the result. In the default
    *                            case, aggregations for all available metadata will be executed. Use
    *                            Exclude irrelevant fields to speed up the execution.
   *
   * @return Result contains aggregation for all available metadata and a subset of nodes that are
   *         prominent for the retrieved subset of documents.
   */
  // TODO Add full-text search parameter
  def search(facets: Map[String, List[String]], excludedAggregations: List[String] = List()): List[Aggregation] = {
    var requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(facets))
      // We are only interested in the document id
      .addFields("id")

    val excluded = defaultExcludedAggregations ++ excludedAggregations
    val validAggregations = aggregationToField.filterKeys(!excluded.contains(_))
    for ((k, v) <- validAggregations) {
      val agg = AggregationBuilders.terms(k)
        .field(v)
        .size(defaultTermSize)
      requestBuilder = requestBuilder.addAggregation(agg)
    }

    val response = requestBuilder.execute().actionGet()
    // There is no need to call shutdown, since this node is the only
    // one in the cluster.
    parseResult(response, validAggregations)
  }

  def scrollTest(facets: Map[String, List[String]]): SearchHitIterator = {
    // val request = QueryBuilders.boolQuery()
    // request.must(QueryBuilders.termQuery("Classification.raw", "CONFIDENTIAL"))

    val requestBuilder = clientService.client.prepareSearch()
      .setQuery(createQuery(facets))
      .addFields("id", "Classification", "Tags")

    // TODO: Check that the response is sorted by score
    new SearchHitIterator(requestBuilder)
  }


  // Maybe have default aggreggationSize for each. Have a button in the frontend show all values.
  // Only by clicking issue a request for all e.g tags. But how do we know, how much?
  /* def search(facets: Map[String, List[String]], aggregationKey: String, size: Int): Aggregation = {

  } */
}

object Testable extends App {

  val facets = Map(
    "Classification" -> List("CONFIDENTIAL"),
    "Tags" -> List("ASEC", "PREL")
  )
  // scalastyle:off
  // TODO: Find out why keywords, and dates are empty.
  // Maybe its not keywords.raw
  // println(FacetedSearch.search(facets, List("Header")))
  // scalastyle:on


  // TODO: id needs to be fieldable :P
  // val hitIterator = FacetedSearch.scrollTest(facets)
  // val fieldIterator = hitIterator.map(hit => hit.field("Classification").getValue[String])
  // fieldIterator.foreach(println(_))
}