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

package database

import scalikejdbc.{SQL, NamedDB}

trait PreparingTables {

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE IF NOT EXISTS document (
                id BIGINT NOT NULL,
                content TEXT NOT NULL,
                created TIMESTAMP NOT NULL,
                CONSTRAINT document_pkey PRIMARY KEY(id)
      )""").execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE entity (
                id BIGINT NOT NULL,
                name CHARACTER VARYING,
                type CHARACTER VARYING,
                frequency INTEGER,
                CONSTRAINT entity_pkey PRIMARY KEY(id)
      )""").execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE IF NOT EXISTS documententity (
                docid BIGINT NOT NULL,
                entityid BIGINT NOT NULL,
                frequency INTEGER NOT NULL,
                CONSTRAINT documententity_pkey PRIMARY KEY(docid, entityid)
      )""").execute.apply()
    }
  } catch { case e: Exception => }
}
