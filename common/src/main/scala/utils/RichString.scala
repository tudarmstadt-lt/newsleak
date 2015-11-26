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

/**
 * Implements additional methods for [[String]].
 * @param underlying the wrapped string.
 */
class RichString(underlying: String) {

  /**
   * Returns the words of the underlying text.
   */
  def words(): List[String] = underlying.split("\\W+").filter(_.nonEmpty).toList
}

/**
 * Companion object for [[RichString]] that provides a convenient method
 * to wrap a instance of [[String]].
 */
object RichString {

  implicit def richString(string: String): RichString = new RichString(string)
}
