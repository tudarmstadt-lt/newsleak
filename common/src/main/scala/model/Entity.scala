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

/**
 * Entity type (<tt>Person</tt>, <tt>Organisation</tt>, <tt>Location</tt>, <tt>Misc</tt>).
 */
object EntityType extends Enumeration {
  val Person = Value
  val Organization = Value
  val Location = Value
  val Misc = Value
}

/**
 * Object representation for Entities.
 *
 * @param id unique id and primary key of the relationship.
 * @param name the entity name.
 * @param frequency the entity's frequency (i.e. co-occurrence) in the underlying data.
 * @param entityType the entity type.
 */
case class Entity(var id: Option[Int] = None, name: String, var frequency: Int = 0, entityType: EntityType.Value)

