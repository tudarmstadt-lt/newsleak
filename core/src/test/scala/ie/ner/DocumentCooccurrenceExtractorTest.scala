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

import java.time.LocalDateTime

import ie.{DocumentCooccurrenceExtractor, GraphBuilder}
import model.{Document, EntityType}
import reader.CorpusReader
import testFactories.FlatSpecWithCommonTraits
import utils.SequentialNumberer

import scala.collection.immutable

class DocumentCooccurrenceExtractorTest extends FlatSpecWithCommonTraits {

  val vertexNumberer = new SequentialNumberer[(String, EntityType.Value)]
  val edgeNumberer = new SequentialNumberer[(Int, Int)]
  class MockableGraphBuilder extends GraphBuilder(vertexNumberer, edgeNumberer)

  /*
      it should "extract entities for each document" in {
    val extractorMock = stub[NamedEntityExtractor]
    val readerMock = mock[CorpusReader]
    val builderMock = mock[MockableGraphBuilder]

    val d = Document(1, "Angela mag ihn.", LocalDateTime.now(), immutable.Map())
    (readerMock.documents _).expects().returning(List(d).toIterable).once()
    // Change to twice for two documents
    // This is for mock[..]
    // (extractorMock.extractNamedEntities _).expects("Angela mag ihn.").returning(List(("Angela", EntityType.Person))).once()

    (extractorMock.extractNamedEntities _).when("Angela mag ihn.").returns(List(("Angela", EntityType.Person)))

    (builderMock.addVertex _).expects(*).anyNumberOfTimes()
    (builderMock.getGraph _).expects().once()

    val uut = new DocumentCooccurrenceExtractor(extractorMock, builderMock)
    uut.extract(readerMock)

    (extractorMock.extractNamedEntities _).verify("Angela mag ihn.")

  }
   */

  it should "extract entities for each document" in {
    val extractorMock = stub[NamedEntityExtractor]
    val readerMock = mock[CorpusReader]
    val builderMock = mock[MockableGraphBuilder]

    // General setups
    val d1 = Document(1, "Angela mag ihn.", LocalDateTime.now(), immutable.Map())
    val d2 = Document(1, "Angela mag ihn nicht.", LocalDateTime.now(), immutable.Map())
    (readerMock.documents _).expects().returning(List(d1, d2).toIterable).once()

    (builderMock.addVertex _).expects(*).anyNumberOfTimes()
    (builderMock.getGraph _).expects().once()

    // Test specific
    (extractorMock.extractNamedEntities _).when("Angela mag ihn.").returns(List(("Angela", EntityType.Person)))
    (extractorMock.extractNamedEntities _).when("Angela mag ihn nicht.").returns(List(("Angela", EntityType.Person)))

    val uut = new DocumentCooccurrenceExtractor(extractorMock, builderMock)
    uut.extract(readerMock)

    (extractorMock.extractNamedEntities _).verify("Angela mag ihn.")
    (extractorMock.extractNamedEntities _).verify("Angela mag ihn nicht.")
  }

  it should "add entities from document" in {
    // val uut = new DocumentCooccurrenceExtractor()
    // uut.extract()
  }
}
