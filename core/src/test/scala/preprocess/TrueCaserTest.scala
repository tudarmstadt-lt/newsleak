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

package preprocess

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import model.Document
import testFactories.FlatSpecWithCommonTraits
import utils.io.IoUtils

class TrueCaserTest extends FlatSpecWithCommonTraits {

  behavior of "TrueCaser"

  it should "return the true case from a given text" in {

    val tf = Map(
      "Machine" -> 10,
      "machine" -> 3,
      "learning" -> 20,
      "Learning" -> 0
    )

    val uut = TrueCaser(tf)
    val actual = uut.applyTrueCasing("MachIne LEaRniNg")

    assert(actual == "Machine learning")
  }

  it should "return the true case from a given document" in {

    val tf = Map(
      "Machine" -> 10,
      "machine" -> 3,
      "learning" -> 20,
      "Learning" -> 0
    )

    val uut = TrueCaser(tf)

    val date: LocalDateTime = LocalDateTime.parse("2007-12-03T10:15:30")
    val document = Document(1, "MachIne LEaRniNg", date, Map())

    val actual = uut.applyTrueCasing(document)
    val expected = Document(1, "Machine learning", date, Map())

    assert(actual == expected)
  }

  it should "take first true case if frequency of multiple cases are equal" in {

    val tf = Map(
      "Single" -> 10,
      "single" -> 10
    )

    val uut = TrueCaser(tf)
    val actual = uut.applyTrueCasing("single")

    assert(actual == "Single")
  }

  it should "return the same word if the word do not have true case" in {

    val tf = Map(
      "Machine" -> 10,
      "Learning" -> 6
    )

    val uut = TrueCaser(tf)
    val actual = uut.applyTrueCasing("NOTHING")

    assert(actual == "NOTHING")
  }

  it should "return the same word if term frequency map is empty" in {

    val uut = TrueCaser(Map[String, Int]())
    val actual = uut.applyTrueCasing("MaChIne")

    assert(actual == "MaChIne")
  }

  behavior of "Companion object TrueCaser"

  val ioUtilsMock = mock[IoUtils]

  it should "return a valid TrueCaser for a given and existing corpus" in {

    val tf = Map(
      "Machine" -> 2,
      "learning" -> 2,
      "is" -> 2,
      "related" -> 1,
      "to" -> 1,
      "statistics" -> 1
    )

    (ioUtilsMock.fromFile[String](_: Path)(_: io.Source => String)).expects(*, *).returning(
      """ Machine learning is
        Machine learning is    related to statistics;
    """
    )

    val expected = TrueCaser(tf)
    val actual = TrueCaser(Paths.get("file"))(ioUtilsMock)

    assert(actual == expected)
  }
}
