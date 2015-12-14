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

import ie.ner.NamedEntityExtractor
import model.graph.CooccurrenceGraph
import model.{Relationship, Entity, EntityType, Document}
import reader.CorpusReader

import scala.collection.mutable

/**
 * Extracts document co-occurrences and builds [[model.graph.CooccurrenceGraph]] from a given input corpus.
 *
 * @param extractor `extractor` is used to extract named-entity instances from each document.
 * @param builder factory to build the [[model.graph.CooccurrenceGraph]].
 */
class DocumentCooccurrenceExtractor(extractor: NamedEntityExtractor, builder: GraphBuilder) {

  /**
   * Creates a [[model.graph.CooccurrenceGraph]] with document co-occurrences for a given corpus.
   *
   * @param sources [[CorpusReader]] handle for the input corpus.
   * @return a [[model.graph.CooccurrenceGraph]] with document co-occurrences.
   */
  def extract(sources: CorpusReader): CooccurrenceGraph = {
    extractCoocurrences(sources.documents)
    builder.getGraph()
  }

  private def extractCoocurrences(documents: Iterable[Document]): Unit = {
    documents.foreach { doc =>
      val result = extractor.extractNamedEntities(doc.content)
      // We count co-occurrences: This means if an entity occurs
      // two or multiple times in a document, we count the observation
      // as one experiment.
      val unique = result.distinct
      val entities = addEntities(unique)
      addRelationships(doc.id, entities)
    }
  }

  private def addEntities(entities: List[(String, EntityType.Value)]): List[Entity] = {
    entities.map {
      case (name, t) =>
        val entity = Entity(name = name, entityType = t)
        builder.addVertex(entity)
    }
  }

  private def addRelationships(docId: Int, entities: List[Entity]): List[Relationship] = {
    val nonDuplicates = entities.combinations(2).filter { case Seq(e1, e2) => e1 != e2 }

    nonDuplicates.map {
      case Seq(e1, e2) =>
        val rel = createRelationship(e1, e2, docId)
        builder.addEdge(rel)
    }.toList
  }

  /**
   * This method creates a relationship with an undefined id and incident entities,
   * where entity1.id is smaller or equal than entity2.id.
   */
  private def createRelationship(e1: Entity, e2: Entity, docId: Int): Relationship = {
    val (first, second) = if (e1.id.get < e2.id.get) (e1, e2) else (e2, e1)
    Relationship(e1 = first.id.get, e2 = second.id.get, docIds = mutable.Set(docId))
  }
}
