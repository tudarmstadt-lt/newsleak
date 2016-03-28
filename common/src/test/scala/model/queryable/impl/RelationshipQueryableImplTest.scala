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

import scalikejdbc.NamedDB
import testFactories.{DatabaseRollback, FlatSpecWithDatabaseTrait}

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class RelationshipQueryableImplTest extends FlatSpecWithDatabaseTrait with DatabaseRollback {

  override def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  // Mocking setup
  final class RelationshipQueryableTestable extends RelationshipQueryableImpl {
    override def connector: NamedDB = testDatabase
  }

  val uut = new RelationshipQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO relationship VALUES (1, 1, 2, 3, false)".update.apply()
    }
  }

  "deleteRelationship" should "set the backlist flag to true" in {
    uut.delete(1)
    val actual = testDatabase.readOnly { implicit session =>
      sql"SELECT isBlacklisted FROM relationship WHERE id = 1".map(_.boolean("isBlacklisted")).single().apply()
    }.getOrElse(fail)

    assert(actual == true)
  }
}
