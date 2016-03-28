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

import model.Tag
import org.scalatest.BeforeAndAfter
import testFactories.FlatSpecWithDatabaseTrait

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class TagQueryableImplTest extends FlatSpecWithDatabaseTrait with BeforeAndAfter {

  def testDatabase: NamedDB = NamedDB('newsleakTestDB)

  // Mocking setup
  final class TagQueryableTestable extends TagQueryableImpl {
    override def connector: NamedDB = testDatabase
  }

  val uut = new TagQueryableTestable

  after {
    // Local cleanup, because we have no common fixture
    testDatabase.localTx { implicit session =>
      sql"TRUNCATE TABLE labels".execute().apply()
      // Reset auto-increment counter
      sql"ALTER SEQUENCE labels_id_seq RESTART WITH 1;".execute.apply()
      sql"TRUNCATE TABLE tags".execute.apply()
      sql"ALTER SEQUENCE tags_id_seq RESTART WITH 1;".execute.apply()
    }
  }

  "add" should "return new created tag if not exists" in {
    val expected = Tag(1, 7, "Document Group 1")
    val actual = uut.add(7, "Document Group 1")

    assert(actual == expected)
  }

  "add" should "return existing tag if already exists" in {
    uut.add(7, "Document Group 1")
    val expected = Tag(1, 7, "Document Group 1")
    val actual = uut.add(7, "Document Group 1")

    assert(actual == expected)
  }

  "delete" should "delete the given tag" in {
    val tag = uut.add(7, "Document Group 1")
    uut.delete(tag.id)
    val actual = uut.getByDocumentId(7)

    assert(actual.isEmpty)
  }

  "delete" should "fail if tag not exist" in {
    val isDeleted = uut.delete(7)

    assert(!isDeleted)
  }

  "delete" should "also remove the label, if the tag is the only one with that label" in {
    uut.add(6, "Document Group 1")
    uut.add(7, "Document Group 1")
    val tag = uut.add(7, "Document Group 2")
    print(tag.id)
    uut.delete(tag.id)

    val actual = testDatabase.readOnly { implicit session =>
      sql"SELECT COUNT(*) AS c FROM labels".map(_.int("c")).single().apply()
    }.getOrElse(fail)

    assert(actual == 1)
  }

  "getDistinctLabels" should "return all labels in the collection" in {
    uut.add(7, "Document Group 1")
    uut.add(7, "Document Group 2")
    uut.add(3, "Document Group 1")

    val expected = List("Document Group 1", "Document Group 2")
    val actual = uut.getDistinctLabels()

    assert(actual == expected)
  }

  "getByDocumentId" should "return all tags for the given document" in {
    uut.add(7, "Document Group 1")
    uut.add(6, "Document Group 3")
    uut.add(7, "Document Group 2")

    val expected = List(
      Tag(1, 7, "Document Group 1"),
      Tag(3, 7, "Document Group 2")
    )
    val actual = uut.getByDocumentId(7)

    assert(actual == expected)
  }
}
