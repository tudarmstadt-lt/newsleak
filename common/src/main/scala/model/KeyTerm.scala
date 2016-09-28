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

import model.queryable.KeyTermQueryable
import model.queryable.impl.KeyTermQueryableImpl
import scalikejdbc.WrappedResultSet
import utils.DBService

case class KeyTerm(term: String, score: Int)

@deprecated("Use ES methods instead")
object KeyTerm {

  def fromDBName(name: String): KeyTermQueryable = new KeyTermQueryableImpl(DBService.dbForName(name))

  def apply(rs: WrappedResultSet): KeyTerm = KeyTerm(rs.string("term"), rs.int("frequency"))
}
