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

package model.queryable.impl

import model.queryable.DocumentQueryable
import model.{Document, TimeExpression}
import org.joda.time.{LocalDate, LocalDateTime}
import utils.{DBService, DBSettings}

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class DocumentQueryableImpl(conn: () => NamedDB) extends DocumentQueryable with DBSettings {

  def connector: NamedDB = conn()

  override def getIds(): List[Long] = connector.readOnly { implicit session =>
    sql"SELECT id FROM document".map(_.long("id")).list.apply()
  }

  override def getById(id: Long): Option[Document] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM document d
          WHERE id = $id
      """.map(Document(_)).toOption().apply()
  }

  override def getByDate(date: LocalDate): List[Document] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM document d
          WHERE created::date = $date
      """.map(Document(_)).list.apply()
  }

  override def getByEntityId(id: Long): List[Document] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM document d
          INNER JOIN documententity de ON d.id = de.docid
          WHERE de.entityid = ${id}
    """.map(Document(_)).list.apply()
  }

  override def getByRelationshipId(id: Long): List[Document] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM document d
          INNER JOIN documentrelationship dr ON d.id = dr.docid
          WHERE dr.relid = ${id}
   """.map(Document(_)).list.apply()
  }

  /* This implementation is significantly faster than <code>getDocumentsByRelationshipId</code> */
  override def getIdsByRelationshipId(id: Long): List[Long] = connector.readOnly { implicit session =>
    sql"""SELECT docId FROM documentrelationship dr
          WHERE dr.relid = ${id}
   """.map(_.long("docId")).list.apply()
  }

  override def getTimeExpressions(docId: Long): List[TimeExpression] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM eventtime t
          WHERE t.docid = ${docId}
       """.map(TimeExpression(_)).list.apply()
  }

  override def getFirstDate(): Option[LocalDateTime] = connector.readOnly { implicit session =>
    sql"""SELECT MIN(created) AS min FROM document""".map(_.jodaLocalDateTime("min")).toOption().apply()
  }

  override def getLastDate(): Option[LocalDateTime] = connector.readOnly { implicit session =>
    sql"""SELECT MAX(created) AS max FROM document""".map(_.jodaLocalDateTime("max")).toOption().apply()
  }

  override def getMetadataKeysAndTypes(): List[(String, String)] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT key, type FROM metadata".map(rs => (rs.string("key"), rs.string("type"))).list.apply()
  }

  override def getMetadataKeyInstances(key: String): List[String] = connector.readOnly { implicit session =>
    sql"SELECT value FROM metadata WHERE key = $key GROUP BY value".map(_.string("value")).list.apply()
  }

  override def getMetadataType(id: Long): List[(String, String)] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT key, type FROM metadata".map(rs => (rs.string("key"), rs.string("type"))).list.apply()
  }

  override def getMetadataValueByDocumentId(docId: Long, key: String): List[String] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT value FROM metadata WHERE docid = ${docId} AND key = ${key}".map(_.string("value")).list.apply()
  }

  override def getMetadataKeyValueByDocumentId(docId: Long): List[(String, String)] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT key, value FROM metadata WHERE docid = ${docId}".map(rs => (rs.string("key"), rs.string("value"))).list.apply()
  }

  override def getMetadataForDocuments(docIds: List[Long], fields: List[String]): List[(Long, String, String)] = connector.readOnly { implicit session =>
    sql"""SELECT m.docid id, m.value, m.key
          FROM metadata m
          WHERE m.key IN (${fields}) AND m.docid IN (${docIds})
      """.map(rs => (rs.long("id"), rs.string("key"), rs.string("value"))).list().apply()
  }
}
