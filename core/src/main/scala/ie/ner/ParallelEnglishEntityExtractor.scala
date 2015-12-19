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

import java.io.BufferedWriter
import java.nio.file.Paths

import epic.preprocess.TreebankTokenizer
import epic.trees.Span
import model.{Document, EntityType}
import org.apache.spark.{SparkConf, SparkContext}
import reader.CorpusReader
import utils.io.IoUtils
import utils.nlp.EnglishNLPUtils

import scala.collection.immutable

class ParallelEnglishEntityExtractor(
    reader: CorpusReader,
    nlpUtils: EnglishNLPUtils = new EnglishNLPUtils,
    ioUtils: IoUtils = new IoUtils
) extends NamedEntityExtractor {

  private val labelToEntityType = immutable.Map(
    "PER" -> EntityType.Person,
    "ORG" -> EntityType.Organization,
    "LOC" -> EntityType.Location,
    "MISC" -> EntityType.Misc
  )

  private val numberOfCores = "8"
  private val sparkContext = new SparkContext(setupSpark("nerExtraction", numberOfCores))
  // Maps sentences to documents
  private val sentenceFileName = "sentences.tsv"
  private val nerFileName = "documents_to_ne.tsv"

  // Maps document ids to named entity tuple
  private lazy val runExtractor: Map[Int, List[(String, EntityType.Value)]] = {
    Map()
  }

  /**
   * @inheritdoc
   */
  override def extractNamedEntities(doc: Document): List[(String, EntityType.Value)] = {
    // The sentence file will be distributed to Spark workers
    ioUtils.withOutput(Paths.get(sentenceFileName)) { writer =>
      writeSentencesToFile(writer)
    }

    // val docToNe = runExtractor
    val tokenizer = new TreebankTokenizer()
    val ner = epic.models.NerSelector.loadNer("en").get

    // We are not using external method calls here, because the class and all usages need to be serializable otherwise.
    // We also use functions instead of methods for call aggregation e.g `extractFromIndexedSentence`.
    // See: http://stackoverflow.com/questions/22592811/task-not-serializable-java-io-notserializableexception-when-calling-function-ou
    val input = sparkContext.textFile(sentenceFileName)

    val extractFromIndexedSentence = (sentence: IndexedSeq[String]) => {
      val segments = ner.bestSequence(sentence)
      val entityTuple = segments.segmentsWithOutside.collect {
        case (Some(l), span: Span) =>
          val ne = segments.words.slice(span.begin, span.end).mkString(" ")
          (ne, l.toString)
      }
      entityTuple.toList
    }

    val docToNe = input.map { line =>
      val Array(docId, sent) = line.split("\t")
      val token = tokenizer(sent)
      (docId, extractFromIndexedSentence(token))
    }

    val res = docToNe.reduceByKey((n, c) => n ++ c).map { case (docId, neLst) => s"$docId\t${neLst.mkString(",")}" }
    res.coalesce(1).saveAsTextFile(nerFileName)

    List()
  }

  private def writeSentencesToFile(out: BufferedWriter): Unit = {
    reader.documents.foreach { doc =>
      val sentences = nlpUtils.segmentText(doc.content)
      sentences.foreach { sent =>
        val line = ioUtils.toTsv(List(doc.id, sent.replace("\t", " ").replace("\n", "")))
        out.write(line)
      }
    }
  }

  /**
   * Initializes the Spark runtime environment for the NER extraction.
   *
   * @param appName job name.
   * @param cores number of cores to be used. Use `*` to use all available cores.
   * @return initialized [[SparkConf]].
   */
  private def setupSpark(appName: String, cores: String): SparkConf = {
    new SparkConf().setAppName(appName).setMaster(s"local[$cores]")
  }
}
