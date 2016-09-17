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

package model.queryable.impl

import model.Relationship
import scalikejdbc.NamedDB
import testFactories.{DatabaseRollback, FlatSpecWithDatabaseTrait}

// scalastyle:off
import scalikejdbc._

class RelationshipQueryableImplTest extends FlatSpecWithDatabaseTrait with DatabaseRollback {

  override def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  // Mocking setup
  final class RelationshipQueryableTestable extends RelationshipQueryableImpl(() => testDatabase)

  val uut = new RelationshipQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      // Graph 1
      // A (blacklisted) <-------> B, C <--- blacklisted ---> D
      RelationshipTestFixture.insertRelationship((1, true), (2, false)) // Rel 1
      RelationshipTestFixture.insertRelationship((3, false), (4, false), true) // Rel 2

      // Graph 2
      // A -> B, B -> C, C -> A
      RelationshipTestFixture.insertRelationship((5, false), (6, false)) // Rel 3
      RelationshipTestFixture.insertRelationship((6, false), (7, false)) // Rel 4
      RelationshipTestFixture.insertRelationship((7, false), (5, false)) // Rel 5
    }
  }

  "deleteRelationship" should "set the blacklist flag to true" in {
    uut.delete(1)
    val actual = testDatabase.readOnly { implicit session =>
      sql"SELECT isBlacklisted FROM relationship WHERE id = 1".map(_.boolean("isBlacklisted")).single().apply()
    }.getOrElse(fail)

    assert(actual)
  }

  "getById" should "return relationship" in {
    val actual = uut.getById(3).getOrElse(fail("Return value shouldn't be None"))
    val expected = Relationship(3, 5, 6, 0)

    assert(actual == expected)
  }

  "getById" should "not return blacklisted relationships" in {
    val actual = uut.getById(2)
    assert(actual.isEmpty)
  }

  "getById" should "not return relationships if one participating entity is blacklisted" in {
    val actual = uut.getById(1)
    assert(actual.isEmpty)
  }

  "getByEntity" should "not return relationship if source entity is blacklisted" in {
    val actual = uut.getByEntity(1)
    assert(actual.isEmpty)
  }

  "getByEntity" should "not return relationship if target entity is blacklisted" in {
    val actual = uut.getByEntity(2)
    assert(actual.isEmpty)
  }

  "getByEntities" should "not return relationship if source entity is blacklisted" in {
    val actual = uut.getByEntities(List(1))
    assert(actual.isEmpty)
  }

  "getByEntities" should "not return relationship if target entity is blacklisted" in {
    val actual = uut.getByEntities(List(2))
    assert(actual.isEmpty)
  }

  "getByEntities" should "induce complete sub graph with no duplicates" in {
    val actual = uut.getByEntities(List(5, 6, 7, 8)).map(_.id)
    val expected = List(3, 4, 5)
    assert(actual == expected)
  }
}
