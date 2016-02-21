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

// scalastyle:off
import model.queryable.RelationshipQueryable
import scalikejdbc._
// scalastyle:on
import utils.{DBSettings, DBRegistry}

/**
 * Representation for relationships.
 *
 * @param id unique id and primary key of the relationship.
 * @param e1 first entity. The order of entities is determined alphabetically. Especially, if (e1, e2) is a
 *           relationship, (e2, e1) is not.
 * @param e2 second entity.
 * @param frequency frequency of the relationship (i.e. co-occurrence) in the underlying data.
 */
case class Relationship(id: Long, e1: Long, e2: Long, var frequency: Int = 0)

/**
 * Companion object for [[model.Relationship]] instances.
 */
object Relationship extends RelationshipQueryable with DBSettings {

  def connector: NamedDB = DBRegistry.db()

  val rowParser = (rs: WrappedResultSet) => Relationship(
    rs.long("id"),
    rs.long("entity1"),
    rs.long("entity2"),
    rs.int("frequency")
  )

  override def getRelationships(): List[Relationship] = connector.readOnly { implicit session =>
    sql"SELECT * FROM relationship".map(rowParser).list.apply()
  }

  override def getRelationshipByEntity(entityId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"SELECT * FROM relationship WHERE entity1 = ${entityId}".map(rowParser).list.apply()
  }

  override def getRelationshipByEntity(entityId: Long, docId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM relationship r
          INNER JOIN documentrelationship dr ON r.id = dr.relid
          WHERE r.entity1 = ${entityId} AND dr.docid = ${docId}
      """.map(rowParser).list.apply()
  }

  override def getRelationshipByDocument(docId: Long): List[Relationship] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM relationship r
          INNER JOIN documentrelationship dr ON r.id = dr.relid
          WHERE dr.docid = ${docId}
      """.map(rowParser).list.apply()
  }
}