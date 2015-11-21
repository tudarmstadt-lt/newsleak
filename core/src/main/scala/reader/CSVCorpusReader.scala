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

package reader

import java.io.IOException
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.LazyLogging
import model.Document

import scala.util.control.NonFatal
import scala.collection.immutable

/**
 * Reads csv files and returns iterable of [[model.Document]]. The csv file should be in the following format
 *
 * <content>, <metadata>
 *
 * where <metadata> consists of triples <key>, <value>, <datatype> separated by a comma.
 *
 * @param file path to the csv file
 */
class CSVCorpusReader(file: Path) extends CorpusReader with LazyLogging {

  /** Csv reader wrapper to parse cable documents */
  private class CSVDocumentReader() extends Iterable[Document] {

    var lastLineParsed = 0
    // Reader expects quoteChar to be '"' as default
    val reader = CSVReader.open(file.toFile)
    val lines = reader.iterator.map { x => { lastLineParsed += 1; x } }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override def iterator: Iterator[Document] = new Iterator[Document] {

      override def hasNext: Boolean = {
        val isNotLastLine = lines.hasNext
        if (!isNotLastLine) {
          reader.close()
        }
        isNotLastLine
      }

      override def next(): Document = {
        try {
          lines.next() match {
            // No metadata provided
            case content :: created :: Nil =>
              val datetime = LocalDateTime.parse(created, formatter)
              Document(lastLineParsed, content, datetime, Map())
            case content :: created :: metaTriple =>
              val datetime = LocalDateTime.parse(created, formatter)
              val metadata = parseMetadata(metaTriple)

              Document(lastLineParsed, content, datetime, metadata)
            case _ => throw new IOException("Line does not match <content>, <date>, <metadata>")
          }
        } catch {
          case NonFatal(exc) =>
            throw new IOException("Parsing failed near line: %d in %s".format(lastLineParsed, file), exc)
        }
      }

      private def parseMetadata(metadata: Seq[String]): immutable.Map[String, (String, List[String])] = {
        // Each three elements form a metadata triple i.e key, value, type
        val keyToValues = metadata.grouped(3).toList.groupBy { case Seq(key, _, _) => key }
        // Interleave data type and aggregate values to a list
        val mapping = keyToValues.mapValues { e =>
          val datatypes = e.map { case Seq(_, _, datatype) => datatype }
          // Multiple values with the same key should share the same (one) data type
          if (datatypes.distinct.size != 1) {
            throw new IOException("Values with same key and different types found.")
          }
          (datatypes.head, e.map { case Seq(_, value, _) => value })
        }
        mapping
      }
    }
  }

  override def documents: Iterable[Document] = {
    logger.info("Read file %s".format(file))
    new CSVDocumentReader()
  }
}
