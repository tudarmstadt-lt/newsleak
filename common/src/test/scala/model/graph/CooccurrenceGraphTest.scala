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

import model.{EntityType, Entity, Relationship}
import testFactories.FlatSpecWithCommonTraits

import scala.collection.mutable

class CooccurrenceGraphTest extends FlatSpecWithCommonTraits {

  "Companion object" should "create an empty graph" in {
    val actual = CooccurrenceGraph.emptyGraph()

    assert(actual.graphRepr.getEdgeCount == 0)
    assert(actual.graphRepr.getVertexCount == 0)
    assert(actual.relationships.isEmpty)
    assert(actual.entities.isEmpty)
  }

  it should "throw an exception if the entity id is None" in {
    val uut = CooccurrenceGraph.emptyGraph()
    val entity = Entity(None, "e1", 0, EntityType.Organization)
    intercept[IllegalArgumentException] {
      uut.addVertex(entity)
    }
  }

  it should "throw an exception if the relationship id is None" in new CooccurrenceGraphFixture {
    val relationship = Relationship(None, e1 = 1, e2 = 2, frequency = 2, docIds = mutable.Set(1))
    intercept[IllegalArgumentException] {
      simpleGraph.addEdge(relationship)
    }
  }

  it should "not add the same entity twice" in new CooccurrenceGraphFixture {
    assert(!simpleGraph.addVertex(e1))
  }

  it should "not add the same relationships twice" in new CooccurrenceGraphFixture {
    assert(!simpleGraph.addEdge(r1))
  }

  it should "not add relationship if entities are not present" in {
    val uut = CooccurrenceGraph.emptyGraph()
    val relationship = Relationship(Some(0), e1 = 0, e2 = 1, frequency = 2, docIds = mutable.Set(1))
    assert(!uut.addEdge(relationship))
  }

  it should "return all edges" in new CooccurrenceGraphFixture {
    val actual = disjunctGraph.getEdges
    val expected = Set(r1, r2)
    assert(actual == expected)
  }

  it should "return all vertices" in new CooccurrenceGraphFixture {
    val actual = simpleGraph.getVertices
    val expected = Set(e1, e2)
    assert(actual == expected)
  }

  it should "count vertices correct" in new CooccurrenceGraphFixture {
    val actual = disjunctGraph.getVertexCount
    val expected = 4
    assert(actual == expected)
  }

  it should "count edges correct" in new CooccurrenceGraphFixture {
    val actual = disjunctGraph.getEdgeCount
    val expected = 2
    assert(actual == expected)
  }

  it should "find vertex" in new CooccurrenceGraphFixture {
    val actual = disjunctGraph.getVertex(0)
    assert(actual == Some(e1))
  }

  it should "find edge" in new CooccurrenceGraphFixture {
    val actual = disjunctGraph.getEdge(0)
    assert(actual == Some(r1))
  }

  // Graph equality tests
}
