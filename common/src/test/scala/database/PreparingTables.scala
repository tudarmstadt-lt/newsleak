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
      SQL("""CREATE TABLE IF NOT EXISTS entity (
                id BIGSERIAL NOT NULL,
                name CHARACTER VARYING,
                type CHARACTER VARYING,
                frequency INTEGER,
                isBlacklisted BOOLEAN DEFAULT FALSE,
                CONSTRAINT entity_pkey PRIMARY KEY(id)
      )""").execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE IF NOT EXISTS relationship (
                id BIGSERIAL NOT NULL,
                entity1 BIGINT NOT NULL,
                entity2 BIGINT NOT NULL,
                frequency INTEGER,
                isBlacklisted BOOLEAN DEFAULT FALSE,
                CONSTRAINT relation_pkey PRIMARY KEY(id)
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

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE IF NOT EXISTS documentrelationship (
                docid BIGINT NOT NULL,
                relid BIGINT NOT NULL,
                frequency INTEGER NOT NULL,
                CONSTRAINT documentrelationship_pkey PRIMARY KEY(docid, relid)
      )""").execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("DROP SEQUENCE IF EXISTS labels_id_seq")
      SQL("CREATE SEQUENCE labels_id_seq start with 1").execute.apply()
      SQL("""CREATE TABLE IF NOT EXISTS labels (
                id BIGINT NOT NULL DEFAULT NEXTVAL('labels_id_seq') PRIMARY KEY,
                label CHARACTER VARYING(255) NOT NULL
      )""").execute.apply()
    }
  } catch { case e: Exception => }

  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      // We use custom sequences instead of build in serial data types.
      // This allows us to vary the auto-increment data type and also
      // reset the sequence for testing.
      SQL("DROP SEQUENCE IF EXISTS tags_id_seq")
      SQL("CREATE SEQUENCE tags_id_seq start with 1").execute.apply()
      SQL(
        """CREATE TABLE IF NOT EXISTS tags (
                id BIGINT NOT NULL DEFAULT NEXTVAL('tags_id_seq') PRIMARY KEY,
                documentid BIGINT NOT NULL,
                labelid BIGINT NOT NULL
          )"""
      ).execute.apply()
    }
  } catch { case e: Exception => }

  // TODO: Primary key?
  try {
    NamedDB('newsleakTestDB).autoCommit { implicit s =>
      SQL("""CREATE TABLE IF NOT EXISTS terms (
                docid BIGINT NOT NULL,
                term CHARACTER VARYING NOT NULL,
                frequency INTEGER NOT NULL
              )""").execute.apply()
    }
  } catch { case e: Exception => }
}
