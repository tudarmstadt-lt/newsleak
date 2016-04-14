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

import model.Document
import org.joda.time.{LocalDate, LocalDateTime}
// scalastyle:off
import scalikejdbc._
// scalastyle:on
import testFactories.{DatabaseRollback, FlatSpecWithDatabaseTrait}

class DocumentQueryableImplTest extends FlatSpecWithDatabaseTrait with DatabaseRollback {

  override def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  final class DocumentQueryableTestable extends DocumentQueryableImpl {
    override def connector: NamedDB = testDatabase
  }

  val uut = new DocumentQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO document VALUES (1, ${"Content of document 1"}, ${LocalDateTime.parse("2007-12-03T10:15:30")})".update.apply()
      sql"INSERT INTO document VALUES (2, ${"Content of document 2"}, ${LocalDateTime.parse("2007-11-03T10:15:30")})".update.apply()

      sql"INSERT INTO documententity VALUES (1, 1, 10)".update.apply()
      sql"INSERT INTO documententity VALUES (2, 1, 3)".update.apply()

      sql"INSERT INTO documententity VALUES (1, 2, 3)".update.apply()
    }
  }

  "getIds" should "return all available ids in the collection" in {
    val expected = List(1, 2)
    val actual = uut.getIds()
    assert(actual === expected)
  }

  "getByDate" should "return only documents for the given date" in {
    val expected = List(
      Document(2, "Content of document 2", LocalDateTime.parse("2007-11-03T10:15:30"))
    )
    val actual = uut.getByDate(LocalDate.parse("2007-11-03"))
    assert(actual === expected)
  }

  "byEntityId" should "return all documents that contain the given entity" in {
    val expected = List(
      Document(1, "Content of document 1", LocalDateTime.parse("2007-12-03T10:15:30")),
      Document(2, "Content of document 2", LocalDateTime.parse("2007-11-03T10:15:30"))
    )
    val actual = uut.getByEntityId(1)
    assert(actual === expected)
  }

  // TODO
  "getByEntityIds" should "only return documents where both entities occur" in {
    val expected = List(Document(1, "Content of document 1", LocalDateTime.parse("2007-12-03T10:15:30")))
    // val actual = Document.getDocumentsByEntityIds(1, 2)
    // assert(actual === expected)
  }

  // ...
}
