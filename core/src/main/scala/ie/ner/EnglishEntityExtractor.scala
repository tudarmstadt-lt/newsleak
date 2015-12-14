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

import epic.trees.Span
import model.EntityType
import utils.nlp.EnglishNLPUtils

import scala.collection.immutable

/**
 * Named Entity tagger based on ScalaNLP CRF NER tagger. The supplied model supports four different
 * classes: <tt>Person</tt>, <tt>Organization</tt>, <tt>Location</tt> and <tt>Misc</tt>. Longer passages
 * will be segmented to sentences and additionally annotated with token annotations before the tagger is applied. Segmentation
 * and tokenization capability is served by `nlpUtils`.
 *
 * @param nlpUtils serves segmentation and tokenization capabilities.
 */
class EnglishEntityExtractor(nlpUtils: EnglishNLPUtils = new EnglishNLPUtils) extends NamedEntityExtractor {

  private val ner = epic.models.NerSelector.loadNer("en").get
  private val labelToEntityType = immutable.Map(
    "PER" -> EntityType.Person,
    "ORG" -> EntityType.Organization,
    "LOC" -> EntityType.Location,
    "MISC" -> EntityType.Misc
  )

  /**
   * @inheritdoc
   */
  override def extractNamedEntities(text: String): List[(String, EntityType.Value)] = {
    val sentences = nlpUtils.segmentText(text).map(nlpUtils.tokenize).toIndexedSeq
    val entities = sentences.flatMap(extractFromIndexedSentence)

    entities.toList
  }

  private def extractFromIndexedSentence(sentence: IndexedSeq[String]): Iterator[(String, EntityType.Value)] = {
    val segments = ner.bestSequence(sentence)

    val entityTuple = segments.segmentsWithOutside.collect {
      case (Some(l), span: Span) =>
        val ne = segments.words.slice(span.begin, span.end).mkString(" ")
        val label = labelToEntityType(l.toString)
        (ne, label)
    }
    entityTuple
  }
}
