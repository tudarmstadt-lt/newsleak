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

package model.faceted.search

import org.joda.time.LocalDateTime

// Builder pattern
/**
 *
 * @param fullTextSearch
 * @param generic
 * @param entities
 * @param fromDate inclusive
 * @param toDate inclusive
 */
case class Facets(
    fullTextSearch: Option[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    fromDate: Option[LocalDateTime],
    toDate: Option[LocalDateTime]
) {

  def isEmpty(): Boolean = fullTextSearch.isEmpty && generic.isEmpty && entities.isEmpty && !hasDateFilter
  def hasDateFilter(): Boolean = fromDate.isDefined || toDate.isDefined
}

