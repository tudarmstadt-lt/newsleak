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

import model.{Entity, EntityType}
import scalikejdbc.NamedDB
import testFactories.{DatabaseRollback, FlatSpecWithDatabaseTrait}

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class EntityQueryableImplTest extends FlatSpecWithDatabaseTrait with DatabaseRollback {

  override def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  // Mocking setup
  final class RelationshipQueryableTestable extends RelationshipQueryableImpl(() => testDatabase) // {
    // override def connector: NamedDB = testDatabase
  // }

  final class EntityQueryableTestable extends EntityQueryableImpl(() => testDatabase) {
    // override def connector: NamedDB = testDatabase
    override val relationship = new RelationshipQueryableTestable
  }

  val uut = new EntityQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO entity VALUES (1, ${"Angela Merkel"}, ${"PER"}, 7, false)".update.apply()
      sql"INSERT INTO entity VALUES (2, ${"Angela Brecht"}, ${"PER"}, 3, false)".update.apply()
      sql"INSERT INTO entity VALUES (3, ${"The Backlist"}, ${"PER"}, 3, true)".update.apply()
      sql"INSERT INTO entity VALUES (4, ${"Angela Merkel"}, ${"ORG"}, 4, false)".update.apply()
      // Angela Brecht occurs in the first document 10 times
      sql"INSERT INTO documententity VALUES (1, 2, 10)".update.apply()
      // Relation: Angela Merkel - Angela Brecht with frequency 3
      sql"INSERT INTO relationship VALUES (1, 1, 2, 3, false)".update.apply()
    }
  }

  "getByName" should "return the entity with the given name" in {
    val expected = List(
      Entity(1, "Angela Merkel", EntityType.Person, 7),
      Entity(4, "Angela Merkel", EntityType.Organization, 4)
    )
    val actual = uut.getByName("Angela Merkel")
    assert(actual === expected)
  }

  // h2 don't supports ilike queries
  ignore should "return entities that share common names" in {
    val expected = List(
      Entity(1, "Angela Merkel", EntityType.Person, 7),
      Entity(2, "Angela Brecht", EntityType.Person, 3),
      Entity(4, "Angela Merkel", EntityType.Organization, 4)
    )
    val actual = uut.getByNamePattern("Angela")
    assert(actual === expected)
  }

  "getById" should "not return blacklisted entities" in {
    assert(None == uut.getById(3))
  }

  "getById" should "return the correct entity" in {
    val expected = Some(Entity(1, "Angela Merkel", EntityType.Person, 7))
    val actual = uut.getById(1)

    assert(actual == expected)
  }

  "getByType" should "return entities corresponding to this type" in {
    val expected = List(
      Entity(1, "Angela Merkel", EntityType.Person, 7),
      Entity(2, "Angela Brecht", EntityType.Person, 3)
    )
    val actual = uut.getByType(EntityType.Person)
    assert(actual == expected)
  }

  "delete" should "set the backlist flag to true" in {
    uut.delete(1)
    val actual = testDatabase.readOnly { implicit session =>
      sql"SELECT isblacklisted FROM entity WHERE id = 1".map(_.boolean("isblacklisted")).single().apply()
    }.getOrElse(fail)

    assert(actual)
  }

  "changeType" should "return false if not successful" in {
    val actual = uut.changeType(7, EntityType.Organization)
    assert(!actual)
  }

  "changeType" should "change entities type to the given one" in {
    uut.changeType(1, EntityType.Organization)
    val actual = testDatabase.readOnly { implicit session =>
      sql"SELECT type FROM entity WHERE id = 1".map(rs => EntityType.withName(rs.string("type"))).single().apply()
    }.getOrElse(fail)

    assert(actual == EntityType.Organization)
  }

  "add" should "return the updated entity if already present" in {
    val expected = Some(Entity(2, "Angela Brecht", EntityType.Person, 4))
    val actual = uut.add(1, "Angela Brecht", EntityType.Person)

    assert(actual == expected)
  }

  "merge" should "blacklist duplicates" in {
    uut.merge(1, List(2))
    assert(uut.getById(2) == None)
  }

  "merge" should "not produce self-referring relationships" in {
    uut.merge(1, List(2))
    assert(uut.relationship.getById(1) == None)
  }
}