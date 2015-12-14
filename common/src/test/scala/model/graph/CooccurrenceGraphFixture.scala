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

import model.{Relationship, EntityType, Entity}

import scala.collection.mutable

trait CooccurrenceGraphFixture {

  val e1 = Entity(Some(0), name = "e1", frequency = 2, entityType = EntityType.Person)
  val e2 = Entity(Some(1), name = "e2", frequency = 2, entityType = EntityType.Location)
  val e3 = Entity(Some(2), name = "e3", frequency = 1, entityType = EntityType.Person)
  val e4 = Entity(Some(3), name = "e4", frequency = 1, entityType = EntityType.Organization)

  val r1 = Relationship(Some(0), e1 = 0, e2 = 1, frequency = 2, docIds = mutable.Set(1, 2))
  val r2 = Relationship(Some(1), e1 = 2, e2 = 3, frequency = 1, docIds = mutable.Set(1, 2))

  lazy val simpleGraph: CooccurrenceGraph = {
    val graph = CooccurrenceGraph.emptyGraph()
    graph.addVertex(e1)
    graph.addVertex(e2)
    graph.addEdge(r1)
    graph
  }

  lazy val disjunctGraph: CooccurrenceGraph = {
    val graph = simpleGraph
    graph.addVertex(e3)
    graph.addVertex(e4)
    graph.addEdge(r2)
    graph
  }
}
