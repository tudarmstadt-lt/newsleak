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

import model.Relationship
import model.queryable.RelationshipQueryable
import utils.DBSettings

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class RelationshipQueryableImpl extends RelationshipQueryable with DBSettings {

  def connector: NamedDB = NamedDB(ConnectionPool.DEFAULT_NAME)

  override def getById(relId: Long): Option[Relationship] = connector.readOnly { implicit session =>
    sql"SELECT * FROM relationship WHERE id = ${relId}".map(Relationship(_)).toOption().apply()
  }

  override def getByEntity(entityId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"SELECT * FROM relationship WHERE entity1 = ${entityId}".map(Relationship(_)).list.apply()
  }

  override def getByEntity(entityId: Long, docId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM relationship r
          INNER JOIN documentrelationship dr ON r.id = dr.relid
          WHERE r.entity1 = ${entityId} AND dr.docid = ${docId}
      """.map(Relationship(_)).list.apply()
  }

  override def getByDocument(docId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM relationship r
          INNER JOIN documentrelationship dr ON r.id = dr.relid
          WHERE dr.docid = ${docId}
      """.map(Relationship(_)).list.apply()
  }

  override def delete(relId: Long): Boolean = connector.localTx { implicit session =>
    val count = sql"UPDATE relationship SET isBlacklisted = TRUE WHERE id = ${relId}".update().apply()
    count == 1
  }
}
