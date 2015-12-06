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

package utils.io

import java.nio.file.Path

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.io.Source

/**
 * Provides common io methods.
 */
class IoUtils extends LazyLogging {

  /**
   *  Opens a file and keeps track of the opened resource i.e. closes it after the operation is finished.
   *  The given function should be strict in the type of T. Otherwise an exception will be thrown, because
   *  the stream is already closed.
   *
   *  @param path file to read.
   *  @param f strict function that will be called on the opened [[io.Source]].
   *  @tparam T return type of the called function.
   */
  def fromFile[T](path: Path)(f: io.Source => T): T = {
    val in = Source.fromFile(path.toFile, "UTF8")
    try {
      f(in)
    } finally {
      in.close()
    }
  }
}
