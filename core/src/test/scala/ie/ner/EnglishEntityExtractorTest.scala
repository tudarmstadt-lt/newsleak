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

import model.EntityType
import testFactories.FlatSpecWithCommonTraits
import utils.nlp.{EnglishNLPUtils, EnglishNLPUtilsTest}

class EnglishEntityExtractorTest extends FlatSpecWithCommonTraits {

  it should "assign per label for a person" in {
    val sentence = "Angela is the german chancellor"

    val uut = whenExtractorIsInitialized(
      List(sentence),
      List(
        List("Angela", "is", "the", "german", "chancellor", ".")
      )
    )

    val expected = List(("Angela", EntityType.Person))
    val actual = uut.extractNamedEntities(sentence)

    assert(expected == actual)
  }

  it should "assign org label for a organization" in {
    val sentence = "She is member of CSU."

    val uut = whenExtractorIsInitialized(
      List(sentence),
      List(
        List("She", "is", "member", "of", "CSU", ".")
      )
    )

    val expected = List(("CSU", EntityType.Organization))
    val actual = uut.extractNamedEntities(sentence)

    assert(expected == actual)
  }

  it should "assign loc label for a location" in {
    val sentence = "She lives of Berlin."

    val uut = whenExtractorIsInitialized(
      List(sentence),
      List(
        List("She", "lives", "in", "Berlin", ".")
      )
    )

    val expected = List(("Berlin", EntityType.Location))
    val actual = uut.extractNamedEntities(sentence)

    assert(expected == actual)
  }

  // TODO: Write misc test
  /**
   * it should "assign misc label for miscellaneous" in {
   * val sentence = ""
   *
   * val uut = whenExtractorIsInitialized(
   * List(sentence),
   * List(
   * List()
   * )
   * )
   *
   * val expected = List(("twitpic.com/999erv", EntityType.Misc))
   * val actual = uut.extractNamedEntities(sentence)
   *
   * assert(expected == actual)
   * }*
   */

  it should "not split multi word expressions" in {
    val sentence = "Angela Merkel is a woman."

    val uut = whenExtractorIsInitialized(
      List(sentence),
      List(
        List("Angela", "Merkel", "is", "a", "woman", ".")
      )
    )

    val expected = List(("Angela Merkel", EntityType.Person))
    val actual = uut.extractNamedEntities(sentence)

    assert(expected == actual)
  }

  it should "extract named entities from multiple sentences" in {
    val s1 = "Angela is the german chancellor."
    val s2 = "She is member of CSU."
    val sentences = List(s1, s2)

    val uut = whenExtractorIsInitialized(
      sentences,
      List(
        List("Angela", "is", "the", "german", "chancellor", "."),
        List("She", "is", "member", "of", "CSU", ".")
      )
    )

    val expected = List(("Angela", EntityType.Person), ("CSU", EntityType.Organization))
    val actual = uut.extractNamedEntities(sentences.mkString(" "))

    assert(expected == actual)
  }

  def whenExtractorIsInitialized(sentences: List[String], sentenceToken: List[List[String]]): EnglishEntityExtractor = {
    val nlpUtilsMock = mock[EnglishNLPUtils]

    (nlpUtilsMock.segmentText _).expects(sentences.mkString(" ")).returning(sentences.toIndexedSeq).anyNumberOfTimes()
    (sentences zip sentenceToken).foreach {
      case (s, t) =>
        (nlpUtilsMock.tokenize _).expects(s).returning(t.toIndexedSeq).once()
    }

    new EnglishEntityExtractor(nlpUtilsMock)
  }
}
