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

package model.graph

import edu.uci.ics.jung.graph.UndirectedSparseMultigraph
import model.{Relationship, Entity}

import scala.collection.mutable

class CooccurrenceGraph private (
    val graphRepr: UndirectedSparseMultigraph[Int, Int],
    val entities: mutable.Map[Int, Entity],
    val relationships: mutable.Map[Int, Relationship]
) {

  def getEdges: Set[Relationship] = relationships.values.toSet
  def getVertices: Set[Entity] = entities.values.toSet

  def getEdge(id: Int): Option[Relationship] = relationships.get(id)
  def getVertex(id: Int): Option[Entity] = entities.get(id)

  def containsEdge(edge: Relationship): Boolean = relationships.contains(edge.id.get)
  def containsVertex(vertex: Entity): Boolean = entities.contains(vertex.id.get)

  def addVertex(vertex: Entity): Boolean = {
    val id = vertex.id.getOrElse(throw new IllegalArgumentException("The value \"None\" for the field id is not allowed."))
    val isAdded = graphRepr.addVertex(id)
    if (isAdded) {
      entities += (id -> vertex)
    }
    isAdded
  }

  def addEdge(edge: Relationship): Boolean = {
    val id = edge.id.getOrElse(throw new IllegalArgumentException("The value \"None\" for the field id is not allowed."))

    val containsVertices = entities.contains(edge.e1) && entities.contains(edge.e2)
    val isAdded = if (containsVertices) graphRepr.addEdge(id, edge.e1, edge.e2) else false

    if (isAdded) {
      relationships += (id -> edge)
    }
    isAdded
  }

  def getEdgeCount: Int = relationships.size
  def getVertexCount: Int = entities.size

  override def equals(other: Any): Boolean = other match {
    case that: CooccurrenceGraph =>
      entities == that.entities && relationships == that.relationships
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(graphRepr, entities, relationships)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def toString: String = s"Graph($entities, $relationships)"
}

object CooccurrenceGraph {

  def emptyGraph(): CooccurrenceGraph = new CooccurrenceGraph(new UndirectedSparseMultigraph[Int, Int](), mutable.Map(), mutable.Map())
}
