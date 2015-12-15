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

package ie.ner

import ie.GraphBuilder
import model.graph.CooccurrenceGraphFixture
import model.{Relationship, EntityType, Entity}
import org.scalatest.BeforeAndAfterEach
import testFactories.FlatSpecWithCommonTraits
import utils.SequentialNumberer

import scala.collection.mutable

class GraphBuilderTest extends FlatSpecWithCommonTraits with BeforeAndAfterEach {

  var uut: GraphBuilder = _

  override def beforeEach(): Unit = {
    uut = new GraphBuilder(new SequentialNumberer[(String, EntityType.Value)], new SequentialNumberer[(Int, Int)])
  }

  it should "increase entity frequency for different entities" in {
    val e1 = Entity(name = "e1", entityType = EntityType.Person)
    val e2 = Entity(name = "e2", entityType = EntityType.Organization)

    val actual = List(uut.addVertex(e1), uut.addVertex(e2))
    val expected = List(
      Entity(Some(0), "e1", 1, EntityType.Person),
      Entity(Some(1), "e2", 1, EntityType.Organization)
    )

    assert(actual === expected)
  }

  "Entity frequency" should "be increased when entity is already present" in {
    val e1 = Entity(name = "e1", entityType = EntityType.Organization)
    val e2 = Entity(name = "e1", entityType = EntityType.Organization)
    uut.addVertex(e1)

    val actual = uut.addVertex(e2)
    val expected = Entity(Some(0), "e1", 2, EntityType.Organization)

    assert(actual === expected)
  }

  "Entity frequency" should "not be increased for entities with the same name and different types" in {
    val e1 = Entity(name = "e1", entityType = EntityType.Person)
    val e2 = Entity(name = "e1", entityType = EntityType.Organization)
    uut.addVertex(e1)

    val actual = uut.addVertex(e2)
    val expected = Entity(Some(1), "e1", 1, EntityType.Organization)

    assert(actual === expected)
  }

  it should "increase relationship frequency for different relationships" in {
    val r1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set())
    val r2 = Relationship(e1 = 1, e2 = 2, docIds = mutable.Set())

    val actual = List(uut.addEdge(r1), uut.addEdge(r2))
    val expected = List(
      Relationship(Some(0), 0, 1, 1, mutable.Set()),
      Relationship(Some(1), 1, 2, 1, mutable.Set())
    )

    assert(actual === expected)
  }

  "Relationship frequency" should "be increased when relationship is already present" in {
    val r1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set())
    val r2 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set())
    uut.addEdge(r1)

    val actual = uut.addEdge(r2)
    val expected = Relationship(Some(0), 0, 1, 2, mutable.Set())

    assert(actual === expected)
  }

  it should "update the document Ids for same relationships" in {
    val r1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set(1, 2, 3))
    val r2 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set(5, 7))
    uut.addEdge(r1)

    val actual = uut.addEdge(r2)
    val expected = Relationship(Some(0), 0, 1, 2, mutable.Set(1, 2, 3, 5, 7))

    assert(actual === expected)
  }

  it should "not update document Ids for different relationships" in {
    val r1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set(1, 2, 3))
    val r2 = Relationship(e1 = 1, e2 = 2, docIds = mutable.Set(5, 7))
    uut.addEdge(r1)

    val actual = uut.addEdge(r2)
    val expected = Relationship(Some(1), 1, 2, 1, mutable.Set(5, 7))

    assert(actual === expected)
  }

  it should "construct correct graph from given entities and relationships" in new CooccurrenceGraphFixture {

    val e1_1 = Entity(name = "e1", entityType = EntityType.Person)
    val e1_2 = Entity(name = "e1", entityType = EntityType.Person)
    val e2_1 = Entity(name = "e2", entityType = EntityType.Location)
    val e2_2 = Entity(name = "e2", entityType = EntityType.Location)
    uut.addVertex(e1_1)
    uut.addVertex(e1_2)
    uut.addVertex(e2_1)
    uut.addVertex(e2_2)

    val r1_1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set(1))
    val r1_2 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set(2))
    uut.addEdge(r1_1)
    uut.addEdge(r1_2)

    val actual = uut.getGraph()
    val expected = simpleGraph

    assert(actual === expected)
  }
}
