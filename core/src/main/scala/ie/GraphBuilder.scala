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

package ie

import com.typesafe.scalalogging.slf4j.LazyLogging
import model.graph.CooccurrenceGraph
import model.{EntityType, Entity, Relationship}
import utils.Numberer

import scala.collection.mutable

/**
 * Factory to create instances of [[model.graph.CooccurrenceGraph]].
 *
 * @param vertexNumberer lookup for [[model.Entity]] ids. Provides the same ids for same
 *                       entities.
 * @param edgeNumberer lookup for [[model.Relationship]] ids.
 */
class GraphBuilder(vertexNumberer: Numberer[(String, EntityType.Value)], edgeNumberer: Numberer[(Int, Int)]) extends LazyLogging {

  // We use AnyRefMap, because it performs faster on get and contain queries
  private val nameToEntity = new mutable.AnyRefMap[(String, EntityType.Value), Entity]()
  private val vertexToRelation = new mutable.HashMap[(Int, Int), Relationship]

  /**
   * Registers a given [[model.Entity]] to the builder. A `vertex` will be registered by
   * its name and type.
   *
   * @param vertex entity to be registered that has no id assigned yet.
   * @return If the `vertex` is already registered, its frequency will be updated and the updated
   *         instance returned. Otherwise an id will be assigned according to the `vertexNumberer`
   *         strategy.
   */
  def addVertex(vertex: Entity): Entity = {
    val name = vertex.name
    val key = (name, vertex.entityType)
    val entity = nameToEntity.getOrElseUpdate(key, {
      vertex.id = Some(vertexNumberer.externalToInternal(key))
      vertex
    })
    // m1 ++= m2.map{ case (k,v) => k -> (v + m2.getOrElse(k,0)) }
    entity.occurrence ++= vertex.occurrence.map { case (k, v) => k -> (v + vertex.occurrence.getOrElse(k, 0)) }
    entity
  }

  /**
   * Registers a given [[model.Relationship]] to the builder. A `edge` will be registered by
   * its connecting nodes.
   *
   * @param edge relationship to be registered that has no id assigned yet.
   * @return If the `edge` is already registered, its frequency and document origin will be updated.
   *         The updated instance will be returned. Otherwise an id will be assigned according to
   *         the `vertexNumberer` strategy.
   */
  def addEdge(edge: Relationship): Relationship = {
    // We use the order of e1.id and e2.id to identify relationships
    require(edge.e1 <= edge.e2)

    val key = (edge.e1, edge.e2)
    val rel = vertexToRelation.getOrElseUpdate(key, {
      edge.id = Some(edgeNumberer.externalToInternal(key))
      edge
    })
    rel.occurrence ++= edge.occurrence.map { case (k, v) => k -> (v + edge.occurrence.getOrElse(k, 0)) }
    // rel.frequency += 1
    // rel.docIds ++= edge.docIds
    rel
  }

  /**
   * Creates a [[model.graph.CooccurrenceGraph]] from the registered edges and vertices.
   *
   * @return [[model.graph.CooccurrenceGraph]] with document co-occurrences.
   */
  def getGraph(): CooccurrenceGraph = {
    logger.info("Loading graph")
    val graph = CooccurrenceGraph.emptyGraph()
    nameToEntity.values.foreach(graph.addVertex)
    vertexToRelation.values.foreach(graph.addEdge)
    logger.info("Graph loaded: %d nodes, %d edges".format(graph.getVertexCount, graph.getEdgeCount))
    graph
  }
}
