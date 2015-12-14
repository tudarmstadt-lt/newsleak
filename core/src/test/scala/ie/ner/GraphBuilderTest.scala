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
import model.{Relationship, EntityType, Entity}
import org.scalatest.BeforeAndAfterEach
import testFactories.FlatSpecWithCommonTraits
import utils.SequentialNumberer

import scala.collection.mutable

class GraphBuilderTest extends FlatSpecWithCommonTraits with BeforeAndAfterEach {

  private var uut: GraphBuilder = _

  override def beforeEach(): Unit = {
    uut = new GraphBuilder(new SequentialNumberer[(String, EntityType.Value)], new SequentialNumberer[(Int, Int)])
  }

  // Test id numbering

  "Vertex frequency" should "be increased when entity is already present" in {
    val e1 = Entity(name = "e1", entityType = EntityType.Organization)
    val e2 = Entity(name = "e1", entityType = EntityType.Organization)
    uut.addVertex(e1)

    val actual = uut.addVertex(e2)
    val expected = Entity(Some(0), "e1", 2, EntityType.Organization)

    assert(actual === expected)
  }

  "Vertex frequency" should "not be increased for entities with the same name and different types" in {
    val e1 = Entity(name = "e1", entityType = EntityType.Person)
    val e2 = Entity(name = "e1", entityType = EntityType.Organization)
    uut.addVertex(e1)

    val actual = uut.addVertex(e2)
    val expected = Entity(Some(1), "e1", 1, EntityType.Organization)

    assert(actual === expected)
  }

  "Edge frequency" should "be increased when relationship is already present" in {
    val r1 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set())
    val r2 = Relationship(e1 = 0, e2 = 1, docIds = mutable.Set())
    uut.addEdge(r1)

    val actual = uut.addEdge(r2)
    val expected = Relationship(Some(0), 0, 1, 2, mutable.Set())

    assert(actual === expected)
  }

  // Test if updates the docIds

  // Test if graph contains correct entities... with correct frequency ..

  // Should not increase frequency for same name but different type
}
