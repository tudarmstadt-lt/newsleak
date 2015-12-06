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

import java.nio.file.Path

import com.typesafe.scalalogging.slf4j.LazyLogging
import model.Document
import utils.Benchmark.toBenchmarkable
import utils.RichString.richString
import utils.io.IoUtils

/**
 * Out of a given corpus, produces a map of true cases. This is a frequency based approach
 * where the common usage of the term is assumed to be a true case.
 *
 * @param termFreq maps terms to its frequency.
 */
case class TrueCaser(termFreq: Map[String, Int]) extends LazyLogging {

  // Maps words in lowercase to its true cases
  private val trueCaseMap: Map[String, String] = calculateTrueCases()

  /**
   * Out of the termFreq map, assign for each word a true case that is the the spelling
   * with the highest frequency.
   */
  private def calculateTrueCases(): Map[String, String] = {
    val group = termFreq.groupBy { case (k, _) => k.toLowerCase }
    val trueCase = group.map {
      case (k, forms) =>
        val (trueCasingForm, _) = forms.maxBy { case (_, freq) => freq }
        k.toLowerCase -> trueCasingForm
    }
    trueCase
  }

  /**
   * Returns the true case for a given text.
   *
   * @param text The input text to get its true case.
   * @return The true case of the input text.
   */
  def applyTrueCasing(text: String): String = {
    val words = text.words()
    val trueCasedWords = words.map { w => trueCaseMap.getOrElse(w.toLowerCase, w) }
    trueCasedWords.mkString(" ")
  }

  /**
   * Returns the true case for a given [[model.Document]].
   *
   * @param document the original document.
   * @return [[model.Document]] with applied true casing.
   */
  def applyTrueCasing(document: Document): Document = {
    val trueCasedContent = applyTrueCasing(document.content)
    Document(document.id, trueCasedContent, document.created, document.metadata)
  }
}

/**
 * Companion object for [[TrueCaser]] that provides factory methods to
 * create instances.
 */
object TrueCaser {

  /**
   * Factory method to create a [[TrueCaser]] from a given corpus file.
   *
   * @param file the corpus file used to determine the term frequency map.
   * @return [[TrueCaser]] derived from a term frequency map consisting of the
   *         words from the given corpus file.
   */
  def apply(file: Path)(ioUtils: IoUtils = new IoUtils): TrueCaser = {
    val tf = createFrequencyMap(file, ioUtils).withBenchmark("Creating term frequency map for TrueCaser...")
    TrueCaser(tf)
  }

  private def createFrequencyMap(file: Path, ioUtils: IoUtils): Map[String, Int] = {
    val content = ioUtils.fromFile(file)(_.mkString(""))
    val words = content.words()
    words.groupBy(identity).map { case (term, counts) => (term, counts.length) }
  }
}
