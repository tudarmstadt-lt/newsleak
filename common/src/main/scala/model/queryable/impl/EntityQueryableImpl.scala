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

import java.time.LocalDate

import model.queryable.EntityQueryable
import model.{Entity, EntityType}
import scalikejdbc.NamedDB
import utils.DBSettings

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class EntityQueryableImpl(conn: () => NamedDB) extends EntityQueryable with DBSettings {

  def connector: NamedDB = conn()
  // Access other QueryableImpl's via class instance rather than the object,
  // because this is how we can loan in database instances for testing.
  val relationship = new RelationshipQueryableImpl(conn)

  override def getById(id: Long): Option[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE id = ${id} AND NOT isblacklisted".map(Entity(_)).toOption().apply()
  }

  override def getByIds(ids: List[Long]): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE id IN (${ids})
                AND NOT isblacklisted
          ORDER BY frequency DESC""".map(Entity(_)).list.apply()
  }

  override def getByName(name: String): List[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE name = ${name} AND NOT isblacklisted".map(Entity(_)).list().apply()
  }

  override def getByNamePattern(name: String): List[Entity] = connector.readOnly { implicit session =>
    val like = s"%$name%"
    sql"SELECT * FROM entity WHERE name ILike ${like} AND NOT isblacklisted ORDER BY frequency DESC".map(Entity(_)).list.apply()
  }

  override def getByType(entityType: EntityType.Value): List[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE type = ${entityType.toString} AND NOT isblacklisted".map(Entity(_)).list.apply()
  }

  override def getByDocId(docId: Long): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT e.id, e.name, e.type, e.frequency FROM entity AS e
          INNER JOIN documententity AS de ON e.id = de.entityid
          WHERE de.docid = ${docId}
          AND NOT e.isblacklisted
       """.map(Entity(_)).list.apply()
  }

  override def getBlacklisted(): List[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE isblacklisted".map(Entity(_)).list.apply()
  }

  override def getDuplicates(): Map[Entity, List[Entity]] = connector.readOnly { implicit session =>
    val duplicates = sql"""SELECT e1.id, e1.name, e1.type, e1.frequency,
                                  e2.id AS focalId, e2.name AS focalName, e2.type AS focalType, e2.frequency AS focalFreq
                           FROM duplicates AS d
                           INNER JOIN entity AS e1 ON e1.id = d.duplicateid
                           INNER JOIN entity AS e2 ON e2.id = d.focalid""".map { rs =>
      (Entity(rs), Entity(
        rs.long("focalId"),
        rs.string("focalName"),
        EntityType.withName(rs.string("focalType")),
        rs.int("focalFreq")
      ))
    }.list.apply()

    duplicates.groupBy { case (_, focalEntity) => focalEntity }.mapValues(_.map(_._1))
  }

  override def getTypes(): List[EntityType.Value] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT type FROM entity WHERE NOT isblacklisted".map(rs => EntityType.withName(rs.string("type"))).list.apply()
  }

  override def getOrderedByFreqAsc(entity: EntityType.Value, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE type = ${entity} AND NOT isblacklisted
          ORDER BY frequency ASC limit ${limit}
      """.map(Entity(_)).list.apply()
  }

  override def getOrderedByFreqAsc(entityType: EntityType.Value, created: LocalDate, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity AS e
          INNER JOIN documententity AS de ON e.id = de.entityid
          INNER JOIN document AS d ON d.id = de.docid
          WHERE e.type = ${entityType.toString} AND d.created = ${created} AND NOT isblacklisted
          ORDER BY e.frequency ASC limit ${limit}
       """.map(Entity(_)).list.apply()
  }

  override def getOrderedByFreqDesc(entityType: EntityType.Value, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE type = ${entityType.toString} AND NOT isblacklisted
          ORDER BY frequency DESC limit ${limit}
    """.map(Entity(_)).list.apply()
  }

  override def getOrderedByFreqDesc(entityType: EntityType.Value, created: LocalDate, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity AS e
          INNER JOIN documententity AS de ON e.id = de.entityid
          INNER JOIN document AS d ON d.id = de.docid
          WHERE e.type = ${entityType.toString} AND d.created = ${created} AND NOT isblacklisted
          ORDER BY e.frequency DESC limit ${limit}
       """.map(Entity(_)).list.apply()
  }

  override def add(documentId: Long, entityName: String, entityType: EntityType.Value): Option[Entity] = connector.autoCommit { implicit session =>

    val eOpt = sql"SELECT id FROM entity WHERE name = ${entityName} AND type = ${entityType.toString}".map(_.long("id")).single.apply()
    // If such an entity already exist, just increase its count. Otherwise, create it.
    val id = eOpt.getOrElse {
      sql"INSERT INTO entity VALUES (${entityName}, ${entityType.toString}, 0, false)".updateAndReturnGeneratedKey().apply()
    }
    sql"UPDATE entity SET frequency = frequency + 1 WHERE id = ${id}".update().apply()

    // Check if entity is already present in this document. If yes, we need to
    // increase its count. Otherwise insert the entity document link with one count.
    val eToDocOpt = sql"SELECT frequency FROM documententity WHERE entityid = ${id} AND docid = ${documentId}".map(_.int("frequency")).single().apply
    if (eToDocOpt.isDefined) {
      sql"UPDATE documententity SET frequency = frequency + 1 WHERE entityid = ${id} AND docid = ${documentId}".update().apply()
    } else {
      sql"INSERT INTO documententity VALUES (${documentId}, ${id}, 1)".update.apply()
    }
    // Return new created entity
    getById(id)
  }

  override def delete(entityId: Long): Boolean = connector.localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = TRUE WHERE id = ${entityId}".update().apply()
    // Successful, if updates one entity
    entityCount == 1
  }

  override def undoDelete(entityId: Long): Boolean = connector.localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = FALSE WHERE id = ${entityId}".update().apply()
    // Remove entity also from the duplicates list
    val duplicateCount = sql"DELETE FROM duplicates WHERE duplicateid = ${entityId}".update().apply()
    // Successful, if updates one entity
    entityCount == 1 && duplicateCount == 1
  }

  override def merge(focalId: Long, duplicates: List[Long]): Boolean = connector.localTx { implicit session =>
    // Keep track of the origin entities for these duplicates
    val merged = duplicates.map { id =>
      sql"INSERT INTO duplicates VALUES (${id}, ${focalId})".update.apply()
      // Blacklist duplicates in order to prevent that they show up in any query
      delete(id)
    }
    // scalastyle:off
    merged.length == duplicates.length && merged.forall(a => true)
    // scalastyle:on
  }

  override def undoMerge(focalId: Long): Boolean = connector.localTx { implicit session =>
    // Remove blacklist flag from all duplicate entries with matching focalId
    sql"""UPDATE entity
          SET isblacklisted = FALSE
          FROM duplicates
          WHERE duplicateid = id AND focalid = ${focalId}""".update().apply()

    sql"DELETE FROM duplicates WHERE focalid = ${focalId}".update().apply()
    // TODO
    true
  }

  override def changeName(entityId: Long, newName: String): Boolean = connector.localTx { implicit session =>
    val count = sql"UPDATE entity SET name = ${newName} WHERE id = ${entityId}".update().apply()
    // Successful, if apply updates one row
    count == 1
  }

  override def changeType(entityId: Long, newType: EntityType.Value): Boolean = connector.localTx { implicit session =>
    val count = sql"UPDATE entity SET type = ${newType.toString} WHERE id = ${entityId}".update().apply()
    count == 1
  }
}