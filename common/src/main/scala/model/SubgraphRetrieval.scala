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

import java.net.InetAddress

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms

// scalastyle:off
class SubgraphRetrieval {

  def nodes(): Unit = {

    val settings = Settings.settingsBuilder()
      .put("cluster.name", "myClusterName").build()
    val client = TransportClient.builder().settings(settings).build()
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("host1"), 9300))

    val facets = Map(
      "Classification" -> List("Confidental", "Secret"),
      "Tags" -> List("asec")
    )

    val request = QueryBuilders.boolQuery()
    facets.map {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        v.map(meta => filter.must(QueryBuilders.termQuery(k, meta)))
        request.must(filter)
    }

    val entitiesAggregation = AggregationBuilders
      .terms("nodes")
      .field("entities.name")
      .size(3)

    val response = client.prepareSearch()
      .setQuery(request)
      .addAggregation(entitiesAggregation)
      .execute().actionGet()

    val result: Terms = response.getAggregations.get("nodes")

    client.close()
  }
}
