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

import com.typesafe.scalalogging.Logger
import org.slf4j.{Logger => Underlying}
import testFactories.FlatSpecWithCommonTraits

class BenchmarkTest extends FlatSpecWithCommonTraits {

  final class BenchmarkableWithMockedLogger[B](mocked: Underlying)(op: => B) extends Benchmarkable(op) {
    override lazy val logger = Logger(mocked)
  }

  val benchmarkFunctionMock = mockFunction[Int, Int]

  it should "execute the given method once with correct parameter" in {
    benchmarkFunctionMock.expects(2).onCall { arg: Int => arg + 1 }.once()
    val uut = new Benchmarkable(benchmarkFunctionMock(2))

    uut.withBenchmark("Message")
  }

  it should "return the result of computation" in {
    benchmarkFunctionMock.expects(*).onCall { arg: Int => arg + 1 }.once()
    val uut = new Benchmarkable(benchmarkFunctionMock(2))

    val actual = uut.withBenchmark("Message")
    assert(actual == 3)
  }

  it should "log the execution time in seconds" in {
    benchmarkFunctionMock.expects(*).onCall { arg: Int => Thread.sleep(1000); arg }.once()
    val loggerMock = mock[Underlying]

    (loggerMock.isInfoEnabled: () => Boolean).expects().returning(true).twice()
    (loggerMock.info(_: String)).expects("Message...").once()
    (loggerMock.info(_: String)).expects("Message... Done. [1 seconds]").once()

    val uut = new BenchmarkableWithMockedLogger(loggerMock)(benchmarkFunctionMock(2))
    uut.withBenchmark("Message")
  }
}
