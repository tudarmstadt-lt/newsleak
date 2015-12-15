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

package utils.nlp

import epic.preprocess.{TreebankTokenizer, MLSentenceSegmenter}

/**
 * Common utility functions to process english text.
 */
class EnglishNLPUtils {

  private val sentenceSplitter = MLSentenceSegmenter.bundled().get
  private val tokenizer = new TreebankTokenizer()

  /**
   * Segments a given text into sentences using ScalaNLP's [[MLSentenceSegmenter]]. Removes also
   * new lines from the segmented sentences and ensures that the first character is capitalized.
   *
   * @param text text to be segmented.
   * @return [[IndexedSeq]] with segmented sentences.
   */
  def segmentText(text: String): IndexedSeq[String] = {
    sentenceSplitter(text).map(_.replaceAll("\n", " ").trim.capitalize)
  }

  /**
   * Tokenizes a given text into token using ScalaNLP's [[TreebankTokenizer]].
   *
   * @param text text to be tokenized.
   * @return [[IndexedSeq]] with token.
   */
  def tokenize(text: String): IndexedSeq[String] = tokenizer(text)
}
