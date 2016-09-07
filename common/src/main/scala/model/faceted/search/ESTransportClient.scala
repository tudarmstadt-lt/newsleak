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

import java.net.InetAddress

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import utils.MyDBs

/**
 * Wrapper around an ElasticSearch {@link TransportClient} node.
 */
class ESTransportClient extends SearchClientService {

  private val clusterName = MyDBs.config.getString("es.clustername")
  private val address = MyDBs.config.getString("es.address")
  private val port = MyDBs.config.getInt("es.port")

  private val settings = Settings.settingsBuilder()
    .put("cluster.name", clusterName).build()

  private lazy val transportClient = TransportClient.builder().settings(settings).build()
    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(address), port))

  override def client: Client = transportClient

  override def shutdown(): Unit = {
    client.close()
  }
}
