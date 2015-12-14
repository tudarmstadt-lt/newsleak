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

package run

import java.nio.file.Paths

import com.typesafe.scalalogging.slf4j.LazyLogging
import ie.{DocumentCooccurrenceExtractor, GraphBuilder}
import ie.ner.EnglishEntityExtractor
import model.EntityType
import model.graph.CooccurrenceGraph
import reader.{CorpusReader, CSVCorpusReader}
import scopt.OptionParser
import utils.{GraphUtils, SequentialNumberer}

/**
 * Used to parse command line arguments.
 *
 * @param sourcefile corpus file to be processed.
 */
case class ArgumentOptions(sourcefile: String = "")

/**
 * Contains the main method to start the processing pipeline.
 */
object Run extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val parser = createOptionParser()
    parser.parse(args, ArgumentOptions()) match {
      case Some(config) =>
        val file = Paths.get(config.sourcefile)
        val reader = new CSVCorpusReader(file)
        val graph = createCooccurrenceGraph(reader)

        new GraphUtils(graph).writeGraphToFile(Paths.get("."))
      case None => parser.showUsage
    }
  }

  private def createCooccurrenceGraph(reader: CorpusReader): CooccurrenceGraph = {
    val vertexNumberer = new SequentialNumberer[(String, EntityType.Value)]
    val edgeNumberer = new SequentialNumberer[(Int, Int)]
    val graphBuilder = new GraphBuilder(vertexNumberer, edgeNumberer)
    val extractor = new DocumentCooccurrenceExtractor(new EnglishEntityExtractor(), graphBuilder)

    extractor.extract(reader)
  }

  private def createOptionParser(): OptionParser[ArgumentOptions] = {
    new OptionParser[ArgumentOptions]("DIVID-DJ") {
      head("DIVID-DJ", "0.1.0-SNAPSHOT")
      help("help").text("prints this usage text.")
      version("version").text("Version 0.1.0-SNAPSHOT")

      opt[String]("sourceFile").required().action { (x, c) =>
        c.copy(sourcefile = x)
      }.text("[required] Path, where the input csv file is located.")
    }
  }
}
