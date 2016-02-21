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

import java.time.LocalDate

import model.queryable.EntityQueryable
import utils.{DBSettings, DBRegistry}

// scalastyle:off
import scalikejdbc._
// scalastyle:on

/**
 * Entity type (<tt>Person</tt>, <tt>Organisation</tt>, <tt>Location</tt>).
 */
object EntityType extends Enumeration {
  val Person = Value
  val Organization = Value
  val Location = Value
}

/**
 * Representation for entities.
 *
 * @param id unique id and primary key of the entity.
 * @param name the entity name.
 * @param entityType the entity type.
 * @param frequency the frequency (i.e. co-occurrence) in the underlying data.
 */
case class Entity(id: Long, name: String, entityType: EntityType.Value, frequency: Int)

/**
 * Companion object for [[model.Entity]] instances.
 */
object Entity extends EntityQueryable with DBSettings {

  def connector: NamedDB = DBRegistry.db()

  val rowParser = (rs: WrappedResultSet) => Entity(
    rs.long("id"),
    rs.string("name"),
    EntityType.withName(rs.string("type")),
    rs.int("frequency")
  )

  override def getEntities(): List[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity".map(rowParser).list.apply()
  }

  override def getEntityTypes(): List[EntityType.Value] = connector.readOnly { implicit session =>
    sql"SELECT DISTINCT type FROM entity".map(rs => EntityType.withName(rs.string("type"))).list.apply()
  }

  override def getEntitiesOrderedByFreqAsc(entity: String, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE type = ${entity}
          ORDER BY frequency ASC limit ${limit}
    """.map(rowParser).list.apply()
  }

  override def getEntitiesOrderedByFreqAsc(entityType: String, created: LocalDate, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity AS e
          INNER JOIN documententity AS de ON e.id = de.entityid
          INNER JOIN document AS d ON d.id = de.docid
          WHERE e.type = ${entityType} AND d.created = ${created}
          ORDER BY e.frequency ASC limit ${limit}
       """.map(rowParser).list.apply()
  }

  override def getEntitiesOrderedByFreqDesc(entity: String, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE type = ${entity}
          ORDER BY frequency DESC limit ${limit}
    """.map(rowParser).list.apply()
  }

  override def getEntitiesOrderedByFreqDesc(entityType: String, created: LocalDate, limit: Int): List[Entity] = connector.readOnly { implicit session =>
    sql"""SELECT * FROM entity AS e
          INNER JOIN documententity AS de ON e.id = de.entityid
          INNER JOIN document AS d ON d.id = de.docid
          WHERE e.type = ${entityType} AND d.created = ${created}
          ORDER BY e.frequency DESC limit ${limit}
       """.map(rowParser).list.apply()
  }

  override def getEntitiesByName(name: String): List[Entity] = connector.readOnly { implicit session =>
    val like = name + "%"
    sql"SELECT * FROM entity WHERE name Like ${like}".map(rowParser).list.apply()
  }

  override def getEntitiesByType(entityType: String): List[Entity] = connector.readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE type = ${entityType}".map(rowParser).list.apply()
  }
}