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

import org.scalatest.BeforeAndAfterAll
import scalikejdbc.NamedDB
import testFactories.FlatSpecWithDatabaseTrait

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class EntityCompanionTest extends FlatSpecWithDatabaseTrait with BeforeAndAfterAll {

  def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  final class EntityQueryableTestable extends EntityQueryableImpl {
    override def connector: NamedDB = testDatabase
  }

  val uut = new EntityQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO entity VALUES (1, ${"Angela Merkel"}, ${"Person"}, 7)".update.apply()
      sql"INSERT INTO entity VALUES (2, ${"Angela Brecht"}, ${"Person"}, 3)".update.apply()
    }
  }

  "getEntitiesByName" should "return entities that share common names" in {
    val expected = List(
      Entity(1, "Angela Merkel", EntityType.Person, 7),
      Entity(2, "Angela Brecht", EntityType.Person, 3)
    )
    val actual = uut.getEntitiesByName("Angela")
    assert(actual === expected)
  }

}
