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

package utils

import java.nio.file.Path

import com.typesafe.scalalogging.slf4j.LazyLogging
import model.graph.CooccurrenceGraph
import utils.io.IoUtils

class GraphUtils(graph: CooccurrenceGraph) extends LazyLogging {

  private val ioUtils = new IoUtils()

  def writeGraphToFile(file: Path): Unit = {
    logger.info(s"Write graph to ${file.toString}")

    // Write entities file
    ioUtils.withOutput(file.resolve("entities.tsv")) { out =>
      graph.getVertices.foreach { e =>
        val line = ioUtils.toTsv(List(e.id, e.name, e.frequency, e.entityType))
        out.write(line)
      }
    }

    // Write relationship file
    ioUtils.withOutput(file.resolve("relationships.tsv")) { out =>
      graph.getEdges.foreach { rel =>
        val line = ioUtils.toTsv(List(rel.id, rel.e1, rel.e2, rel.frequency))
        out.write(line)
      }
    }
  }
}
