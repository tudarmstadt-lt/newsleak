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

import scalikejdbc.{ConnectionPool, NamedDB}

/**
 * This object is used to handle the connection to the database. It retrieves
 * the default connection from the connection pool if no database instance
 * is set. Can also be used to loan in database stubs for testing.
 */
object DBRegistry {

  private var databaseConnection: Option[NamedDB] = {
    val db = NamedDB(ConnectionPool.DEFAULT_NAME)
    Some(db)
  }

  /**
   * Returns a database instance.
   *
   * @return current registered database instance. If non is explicit set, it
   *         will return the default connection from the connection pool.
   */
  def db(): NamedDB = databaseConnection.get

  /**
   * Overrides the current registered database instance with the given one.
   *
   * @param db database instance to be registered.
   */
  def registerDB(db: NamedDB): Unit = databaseConnection = Some(db)
}
