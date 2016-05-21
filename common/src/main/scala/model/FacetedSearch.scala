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

package model

// scalastyle:off
import java.net.InetAddress

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import scala.collection.JavaConversions._

// Note doc counts are approximated. See:
// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html
sealed abstract class Bucket
// Use Pattern matching for identification in the frontend
// TODO: Provide Json formatter for each case class for the frontend?
case class MetaDataBucket(key: String, docCount: Long) extends Bucket
// TODO: It would be cool to retrieve the other field for entities from aggregation too.
// Otherwise, we need to issue a second query.
case class NodeBucket(id: Long, docCount: Long) extends Bucket
case class Aggregation(key: String, buckets: List[Bucket])


// Take a look at BoolQueryParser. Converts String input to BoolQuery
object FacetedSearch extends App {

  private val nodesIndex = "nodes"
  // Generic aggregations that need to be retrieved from the metadata
  // We need to return all meta data aggregations, because we have charts for all.
  val metaAggregationRequest = Map(
    nodesIndex -> "entities.entId",
    "signedby_agg" -> "SignedBy.raw",
    "classification_agg" -> "Classification.raw"
    // ...
  )

  // TODO: Move Sessions to application.conf and parse
  private val settings = Settings.settingsBuilder()
      .put("cluster.name", "NewsLeaksCluster").build()
  private val client = TransportClient.builder().settings(settings).build()
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9501))


  private def createQuery(facets:  Map[String, List[String]]): BoolQueryBuilder = {
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

  // Convert response to internal model
  private def parseResult(response: SearchResponse): List[Aggregation] = {
    val res = metaAggregationRequest.collect {
        // Create node bucket for entities
        case(k, v) if k == nodesIndex =>
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

  // facets are parameter, multiple parameters will be joined via "and".
  // We definitely need to extend this, once we have a concept for other
  // logical filter in the frontend.
  def search(facets:  Map[String, List[String]]): List[Aggregation] = {

    var requestBuilder = client.prepareSearch()
      .setQuery(createQuery(facets))

    // Add other aggregations that are generic
    for((k, v) <- metaAggregationRequest) {
      val agg = AggregationBuilders.terms(k)
        .field(v)
        .size(10)
      requestBuilder = requestBuilder.addAggregation(agg)
    }

    val response = requestBuilder.execute().actionGet()
    println(response)


    client.close()
    parseResult(response)
  }


  val facets = Map(
    "Classification" -> List("CONFIDENTIAL"),
    "Tags" -> List("ASEC")
  )

  search(facets)
}


/*def sample(): Unit = {
    val request = QueryBuilders.boolQuery()
      //.must(QueryBuilders.termQuery("Classification.raw", "UNCLASSIFIED"))
      .must(QueryBuilders.termQuery("SignedBy.raw", "CLINTON"))

    val responseBuilder = client.prepareSearch()
      .setQuery(request)

    val response = responseBuilder.execute().actionGet()
    println(response)
  }*/
