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

package model.queryable

import java.time.LocalDate

import model.{Entity, EntityType}

trait EntityQueryable {

  /**
   * Returns all [[model.Entity]]s.
   *
   * @return List of all [[model.Entity]]
   */
  def getEntities(): List[Entity]

  /**
   * Returns all available entity types.
   * @return List[EntityType]
   */
  def getEntityTypes(): List[EntityType.Value]

  /**
   * Get list of [[Entity]]s in ascending order.
   *
   * @param entityType   the EntityType
   * @return List of sorted [[model.Entity]]
   * @param limit  the number of [[model.Entity]] to return
   */
  def getEntitiesOrderedByFreqAsc(entityType: String, limit: Int): List[Entity]

  /**
   * Get list of sorted [[model.Entity]]s in ascending order on a specific date
   *
   * @param entityType   the EntityType
   * @param created the date to filter {{{Entity}}}
   * @return List of sorted [[model.Entity]]
   * @param limit   the number of [[model.Entity]] to return
   */
  def getEntitiesOrderedByFreqAsc(entityType: String, created: LocalDate, limit: Int): List[Entity]

  /**
   * Get list of [[Entity]]s in descending order.
   *
   * @param entityType the EntityType
   * @return List of sorted [[model.Entity]]
   * @param limit the number of [[model.Entity]] to return
   */
  def getEntitiesOrderedByFreqDesc(entityType: String, limit: Int): List[Entity]

  /**
   * Get list of sorted [[model.Entity]]s in descending order on a specific date
   *
   * @param entityType   the EntityType
   * @param created the date to filter [[model.Entity]]
   * @return List of sorted [[model.Entity]]
   * @param limit   the number of [[model.Entity]] to return
   */
  def getEntitiesOrderedByFreqDesc(entityType: String, created: LocalDate, limit: Int): List[Entity]

  /**
   * Get list of [[model.Entity]]s with a given name pattern.
   *
   * @param name name The name of an [[model.Entity]] to be searched
   * @return List of  [[model.Entity]]
   */
  def getEntitiesByName(name: String): List[Entity]
  /**
   * Get list of [[model.Entity]]s with a given type such as `PER`, `LOC`, `ORG` or `MIS`.
   *
   * @param entityType The type of an [[model.Entity]] to be searched
   * @return List of [[model.Entity]]
   */
  def getEntitiesByType(entityType: String): List[Entity]
}
