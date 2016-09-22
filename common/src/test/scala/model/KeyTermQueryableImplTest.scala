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

import model.queryable.impl.KeyTermQueryableImpl
import org.scalatest.BeforeAndAfterAll
import testFactories.FlatSpecWithDatabaseTrait
// scalastyle:off
import scalikejdbc._
// scalastyle:on

class KeyTermQueryableImplTest extends FlatSpecWithDatabaseTrait with BeforeAndAfterAll {

  def testDatabase: NamedDB = NamedDB('newsleakTestDB)
  val uut = new KeyTermQueryableImpl(() => testDatabase)

  override def beforeAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"INSERT INTO terms VALUES (1, ${"CDU"}, 7)".update.apply()
      sql"INSERT INTO terms VALUES (1, ${"SPD"}, 3)".update.apply()

      sql"INSERT INTO documentrelationship VALUES (1, 1, 10)".update.apply()
    }
  }

  "getDocumentKeyTerms" should "return all terms if no limit is set" in {
    val expected = List(
      KeyTerm("CDU", 7),
      KeyTerm("SPD", 3)
    )
    val actual = uut.getDocumentKeyTerms(1)

    assert(actual == expected)
  }

  "getDocumentTermVectors" should "return only limit-terms" in {
    val actual = uut.getDocumentKeyTerms(1, Some(1))

    assert(actual.size == 1)
  }

  "getDocumentTermVectors" should "return ordered by importance when limit" in {
    val expected = List(
      KeyTerm("CDU", 7)
    )
    val actual = uut.getDocumentKeyTerms(1, Some(1))

    assert(actual == expected)
  }

  "getRelationshipKeyTerms" should "return important terms with document occurrence" in {
    val expected = List(
      KeyTerm("CDU", 1),
      KeyTerm("SPD", 1)
    )
    val actual = uut.getRelationshipKeyTerms(1)

    assert(actual == expected)
  }
}
