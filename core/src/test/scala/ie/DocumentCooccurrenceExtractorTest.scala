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

package ie

import java.time.LocalDateTime

import ie.ner.NamedEntityExtractor
import org.scalatest.OneInstancePerTest
// scalastyle:off
import model.EntityType._
// scalastyle:on
import model.{Document, Entity, Relationship}
import reader.CorpusReader
import testFactories.FlatSpecWithCommonTraits
import utils.SequentialNumberer

import scala.collection.{immutable, mutable}

class DocumentCooccurrenceExtractorTest extends FlatSpecWithCommonTraits with OneInstancePerTest {

  val vertexNumberer = new SequentialNumberer[(String, Value)]
  val edgeNumberer = new SequentialNumberer[(Int, Int)]
  sealed class MockableGraphBuilder extends GraphBuilder(vertexNumberer, edgeNumberer)

  trait ReaderMockFixture {
    var readerMock = mock[CorpusReader]
    // General setup
    val d1 = Document(1, "Angela mag ihn.", LocalDateTime.now(), immutable.Map())
    val d2 = Document(2, "Peter ist in Frankfurt und Peter mag es.", LocalDateTime.now(), immutable.Map())
    (readerMock.documents _).expects().returning(List(d1, d2).toIterable).once()
  }

  /* trait GraphBuilderStubFixture extends ReaderMockFixture {
    val extractorMock = mock[NamedEntityExtractor]
    val builderMock = stub[MockableGraphBuilder]

    // Common setup
    (builderMock.getGraph _).when().once()

    (extractorMock.extractNamedEntities _).expects("Angela mag ihn.").returning(
      List(("Angela", Person))
    ).once()
    (extractorMock.extractNamedEntities _).expects("Peter ist in Frankfurt und Peter mag es.").returning(
      List(("Peter", Person), ("Frankfurt", Location), ("Peter", Person))
    ).once()

    val uut = new DocumentCooccurrenceExtractor(extractorMock, builderMock)
  }

  it should "extract entities for every document" in new ReaderMockFixture {
    val extractorMock = stub[NamedEntityExtractor]
    val builderMock = mock[MockableGraphBuilder]

    (builderMock.addVertex _).expects(*).anyNumberOfTimes()
    (builderMock.getGraph _).expects().once()

    (extractorMock.extractNamedEntities _).when("Angela mag ihn.").returns(List(("Angela", Person)))
    (extractorMock.extractNamedEntities _).when("Peter ist in Frankfurt und Peter mag es.").returns(
      List(("Peter", Person), ("Frankfurt", Location))
    )

    val uut = new DocumentCooccurrenceExtractor(extractorMock, builderMock)
    uut.extract(readerMock)

    (extractorMock.extractNamedEntities _).verify("Angela mag ihn.")
    (extractorMock.extractNamedEntities _).verify("Peter ist in Frankfurt und Peter mag es.")
  }

  it should "add entities for every document" in new GraphBuilderStubFixture {
    uut.extract(readerMock)
    (builderMock.addVertex _).verify(Entity(name = "Angela", entityType = Person))
    (builderMock.addVertex _).verify(Entity(name = "Peter", entityType = Person))
    (builderMock.addVertex _).verify(Entity(name = "Frankfurt", entityType = Location))
  }

  it should "add relationships for every document" in new GraphBuilderStubFixture {
    (builderMock.addVertex _).when(Entity(name = "Angela", entityType = Person)).returning(Entity(Some(0), "Angela", 0, Person))
    (builderMock.addVertex _).when(Entity(name = "Peter", entityType = Person)).returning(Entity(Some(1), "Peter", 0, Person))
    (builderMock.addVertex _).when(Entity(name = "Frankfurt", entityType = Location)).returning(Entity(Some(2), "Frankfurt", 0, Location))

    uut.extract(readerMock)
    (builderMock.addEdge _).verify(Relationship(e1 = 1, e2 = 2, docIds = mutable.Set(2)))
  }*/
}
