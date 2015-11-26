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

import java.util.concurrent.TimeUnit.NANOSECONDS

import com.typesafe.scalalogging.LazyLogging

import scala.language.implicitConversions

/**
 * Provides a method to wrap operations and measure there execution time.
 *
 * @param op method to measure the execution time.
 * @tparam B return type of the given method.
 */
class Benchmarkable[B](op: => B) extends LazyLogging {

  /**
   * Given a message, logs it at the beginning of the operation, performs the operation, logs
   * the message again with the time (in seconds) it took to execute the operation, and returns
   * the operation's result.
   *
   * Use like this:
   *
   *   ([block of code]).withBenchmark("Performing operation")
   *
   * This will result in the output:
   *
   *   """
   *   Performing operation...
   *   Performing operation... [n seconds]
   *   """
   *
   *   @param message that will be logged
   */
  def withBenchmark(message: String): B = {
    logger.info(message + "...")
    val start = System.nanoTime
    val result = op
    val end = System.nanoTime
    val seconds = NANOSECONDS.toSeconds(end - start)
    logger.info("%s... Done. [%d seconds]".format(message, seconds))

    result
  }
}

/**
 * Companion object for [[Benchmarkable]] that provides a convenient method
 * to measure execution time.
 */
object Benchmark {

  implicit def toBenchmarkable[B](op: => B): Benchmarkable[B] = new Benchmarkable(op)
}
