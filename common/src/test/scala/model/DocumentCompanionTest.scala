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

import org.joda.time.LocalDateTime
import org.scalatest.BeforeAndAfterAll
// scalastyle:off
import scalikejdbc._
// scalastyle:on
import testFactories.FlatSpecWithDatabaseTrait

class DocumentCompanionTest extends FlatSpecWithDatabaseTrait with BeforeAndAfterAll {

  def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  final class DocumentQueryableTestable extends DocumentQueryableImpl {
    override def connector: NamedDB = testDatabase
  }

  val uut = new DocumentQueryableTestable

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO document VALUES (1, ${"Content of document 1"}, ${LocalDateTime.parse("2007-12-03T10:15:30")})".update.apply()
      sql"INSERT INTO document VALUES (2, ${"Content of document 2"}, ${LocalDateTime.parse("2007-12-03T10:15:30")})".update.apply()

      sql"INSERT INTO documententity VALUES (1, 1, 10)".update.apply()
      sql"INSERT INTO documententity VALUES (2, 1, 3)".update.apply()

      sql"INSERT INTO documententity VALUES (1, 2, 3)".update.apply()
    }
  }

  "getDocuments" should "return all available documents in the collection" in {
    val expected = List(
      Document(1, "Content of document 1", LocalDateTime.parse("2007-12-03T10:15:30")),
      Document(2, "Content of document 2", LocalDateTime.parse("2007-12-03T10:15:30"))
    )
    val actual = uut.getDocuments()
    assert(actual === expected)
  }

  "getDocumentIds" should "return all available ids in the collection" in {
    val expected = List(1, 2)
    val actual = uut.getDocumentIds()
    assert(actual === expected)
  }

  "getDocumentsByEntityId" should "return all documents that contain the given entity" in {
    val expected = List(
      Document(1, "Content of document 1", LocalDateTime.parse("2007-12-03T10:15:30")),
      Document(2, "Content of document 2", LocalDateTime.parse("2007-12-03T10:15:30"))
    )
    val actual = uut.getDocumentsByEntityId(1)
    assert(actual === expected)
  }

  // TODO
  "getDocumentsByEntityIds" should "only return documents where both entities occur" in {
    val expected = List(Document(1, "Content of document 1", LocalDateTime.parse("2007-12-03T10:15:30")))
    // val actual = Document.getDocumentsByEntityIds(1, 2)
    // assert(actual === expected)
  }

  // ...
}
