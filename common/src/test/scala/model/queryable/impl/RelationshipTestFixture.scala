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

// scalastyle:off
import scalikejdbc._

object RelationshipTestFixture {

  type EntityPair = (Long, Boolean)
  private var relationshipIndex = 1

  def insertRelationship(from: EntityPair, to: EntityPair, isBlacklisted: Boolean = false, freq: Int = 0)(implicit session: DBSession): Relationship = {
    val (fromIndex, fromBlacklist) = from
    val (toIndex, toBlacklist) = to
    val rel = Relationship(relationshipIndex, fromIndex, toIndex, freq)

    sql"""
          INSERT INTO relationship
          VALUES (${relationshipIndex}, ${fromIndex}, ${toIndex}, ${freq}, ${isBlacklisted})
       """.update.apply()
    insertEntity(fromIndex, fromBlacklist)
    insertEntity(toIndex, toBlacklist)

    relationshipIndex += 1
    rel
  }

  private def insertEntity(index: Long, isBlacklisted: Boolean)(implicit session: DBSession): Unit = {
    val idOpt = sql"SELECT id FROM entity WHERE id = ${index}".map(_.long("id")).toOption().apply()
    idOpt.getOrElse {
      sql"""
        INSERT INTO entity
        VALUES (${index}, ${"Dummy Entity"}, ${"PER"}, 1, ${isBlacklisted})
      """.update.apply()
    }
  }
}
