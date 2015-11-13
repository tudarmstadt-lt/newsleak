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

import java.time.LocalDateTime

import scala.collection.immutable

/**
 * Common document representation
 *
 * @param id unique document identifier
 * @param content document body that contains raw text
 * @param created creation date of the document
 * @param metadata returns a maybe empty map that maps from keys to a tuple (x, y), where x refers to
 * the type of the meta data associated wih that key and y represents the respective list of meta data values.
 */
case class Document(
  val id: Int,
  val content: String,
  val created: LocalDateTime,
  val metadata: immutable.Map[String, (String, List[String])]
)
