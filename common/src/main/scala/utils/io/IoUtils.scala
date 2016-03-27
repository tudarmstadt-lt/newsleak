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

import java.io.BufferedWriter
import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.io.Source

/**
 * Provides common io methods.
 */
class IoUtils extends LazyLogging {

  val Tab = "\t"
  val Newline = "\n"
  val Utf8 = "UTF-8"

  /**
   *  Opens a file and keeps track of the opened resource i.e. closes it after the operation is finished.
   *  The given function should be strict in the type of T. Otherwise an exception will be thrown, because
   *  the stream is already closed.
   *
   *  @param path file to read.
   *  @param f strict function that will be called on the opened [[scala.io.Source]].
   *  @tparam T return type of the called function.
   */
  def fromFile[T](path: Path)(f: scala.io.Source => T): T = {
    val in = Source.fromFile(path.toFile, "UTF8")
    try {
      f(in)
    } finally {
      in.close()
    }
  }

  def withOutputs[B](paths: Path*)(op: Seq[BufferedWriter] => B): B = {
    val outs = for (path <- paths)
      yield Files.newBufferedWriter(path, Charset.forName(Utf8))
    try {
      op(outs)
    } finally {
      for (out <- outs) out.close()
    }
  }

  def withOutput[B](path: Path)(op: BufferedWriter => B): B = {
    withOutputs(path) {
      case Seq(out) =>
        op(out)
    }
  }

  def toTsv(fields: List[Any]): String = {
    def filter(l: List[Any]): List[Any] = {
      l.collect {
        case Nil => Nil
        case Some(x) => x
        case Some(x) :: xs => x :: filter(xs)
        case x :: xs => x :: filter(xs)
        case x: Any if x != None => x
      }
    }
    filter(fields).mkString(Tab) + Newline
  }
}
