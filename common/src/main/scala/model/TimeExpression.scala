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

package model

import scalikejdbc.WrappedResultSet

/**
 * TimeX3 types (<tt>Date</tt>, <tt>Time</tt>, <tt>Duration</tt>, <tt>Set</tt>).
 *
 * * Date: The expression describes a calendar time.
 *         Examples:
 *         Mr. Smith left Friday, October 1, 1999.
 *                        yesterday.
 *                        two weeks from next Tuesday.
 * * Duration: The expression describes a duration e.g Mr. Smith stayed 2 months in Boston.
 * * Time: The expression refers to a time of the day, even if in a very
 *         indefinite way.
 *         Examples:
 *         Mr. Smith left ten minutes to three.
 *                        late last night.
 *                        the morning of January 31.
 *
 * * Set: The expression describes a set of times e.g John swims twice a week.
 *
 * See: http://www.timeml.org/tempeval2/tempeval2-trial/guidelines/timex3guidelines-072009.pdf
 */
object TimeType extends Enumeration {
  val Date = Value("DATE")
  val Time = Value("TIME")
  val Duration = Value("DURATION")
  val Set = Value("SET")
}

case class TimeExpression(
  docId: Long,
  startOffset: Int,
  endOffset: Int,
  mention: String,
  timeType: TimeType.Value,
  resolution: String
)

object TimeExpression {

  def apply(rs: WrappedResultSet): TimeExpression = TimeExpression(
    rs.long("docid"),
    rs.int("beginoffset"),
    rs.int("endoffset"),
    rs.string("timex"),
    TimeType.withName(rs.string("type")),
    rs.string("timexvalue")
  )
}
