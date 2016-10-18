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
   * Returns the [[model.Entity]] associated with the given id.
   *
   * @param id entity id
   * @return Some([[model.Entity]]) if the given entity exists.
   *         None otherwise.
   */
  def getById(id: Long): Option[Entity]

  /**
   * Returns a ordered list of [[model.Entity]] associated with the given ids. The list
   * is ordered according to the decreasing frequency of the entities in the underlying
   * dataset.
   *
   * @param ids list of entity ids
   * @return Non empty list of [[model.Entity]] if at least one entity exists.
   *         Nil otherwise.
   */
  def getByIds(ids: List[Long]): List[Entity]

  /**
   * Returns a list of entities with the given name. This is an exact match!
   * The result can also contain entities that share the same name, but have
   * different types like (Angela, PER) and (Angela, ORG).
   *
   * @param name name The name of an [[model.Entity]] to be searched
   * @return  List of [[model.Entity]] if the entity matches the given name
   *          Empty list otherwise.
   */
  def getByName(name: String): List[Entity]

  /**
   * Get list of [[model.Entity]]s with a given name pattern. For example
   * "Angela" will match "Angela Merkel", "Angela Brecht", ...
   *
   * @param name name The name of an [[model.Entity]] to be searched
   * @return List of [[model.Entity]]
   */
  def getByNamePattern(name: String): List[Entity]

  /**
   * Get list of [[model.Entity]]s with a given type such as `PER`, `LOC`, `ORG` or `MIS`.
   *
   * @param entityType The type of an [[model.Entity]] to be searched
   * @return List of [[model.Entity]]
   */
  def getByType(entityType: EntityType.Value): List[Entity]

  def getByDocId(docId: Long): List[Entity]

  def getBlacklisted(): List[Entity]

  def getDuplicates(): Map[Entity, List[Entity]]

  /**
   * Returns all available entity types.
   * @return List[EntityType]
   */
  def getTypes(): List[EntityType.Value]

  /**
   * Get list of [[Entity]]s in ascending order.
   *
   * @param entityType  the EntityType
   * @param limit  the number of [[model.Entity]] to return
   * @return List of sorted [[model.Entity]]
   */
  def getOrderedByFreqAsc(entityType: EntityType.Value, limit: Int): List[Entity]

  /**
   * Get list of sorted [[model.Entity]]s in ascending order on a specific date
   *
   * @param entityType   the EntityType
   * @param created the date to filter {{{Entity}}}
   * @param limit   the number of [[model.Entity]] to return
   * @return List of sorted [[model.Entity]]
   */
  def getOrderedByFreqAsc(entityType: EntityType.Value, created: LocalDate, limit: Int): List[Entity]

  /**
   * Get list of [[Entity]]s in descending order.
   *
   * @param entityType the EntityType
   * @param limit the number of [[model.Entity]] to return
   * @return List of sorted [[model.Entity]]
   */
  def getOrderedByFreqDesc(entityType: EntityType.Value, limit: Int): List[Entity]

  /**
   * Get list of sorted [[model.Entity]]s in descending order on a specific date
   *
   * @param entityType   the EntityType
   * @param created the date to filter [[model.Entity]]
   * @param limit the number of [[model.Entity]] to return
   * @return List of sorted [[model.Entity]]
   */
  def getOrderedByFreqDesc(entityType: EntityType.Value, created: LocalDate, limit: Int): List[Entity]

  // PUT Methods

  // TODO: Should this also trigger relationship updates?
  /**
   * Adds a new entity with `entityName` and `entityType` to the
   * document referred by the given `documentId`. The method only
   * adds and counts the marked entity. Multiple occurrences
   * of the given `entityName` in the document won't be counted.
   *
   * @param documentId document
   * @param entityName new entity name
   * @param entityType new type name
   * @return Some(entity) if successful. None, otherwise.
   */
  def add(documentId: Long, entityName: String, entityType: EntityType.Value): Option[Entity]

  /**
   * Marks the given entity as blacklisted. Blacklisted entities won't be
   * returned from any public storage access method. It also blacklists
   * all adjacent relationships.
   *
   * @param entityId entity to blacklist
   * @return <code>True</code> if successful. <code>False</code>, otherwise.
   */
  def delete(entityId: Long): Boolean

  def undoDelete(entityId: Long): Boolean
  /**
   * Blacklists the given `duplicates` and updates the frequency of
   * the entity associated with the given `focalId` with the
   * sum of the duplicated entity frequencies.
   *
   * @param focalId target merge entity
   * @param duplicates entities to remove
   * @return <code>True</code> if successful. <code>False</code>, otherwise.
   */
  def merge(focalId: Long, duplicates: List[Long]): Boolean

  def undoMerge(focalId: Long): Boolean

  /**
   * Changes the Name for an entity associated with the
   * given key.
   *
   * @param entityId entity id.
   * @param newName new name that should be stored.
   * @return <code>True</code>, if successful. <code>False</code> otherwise.
   */
  def changeName(entityId: Long, newName: String): Boolean

  /**
   * Changes the [[EntityType]] for an entity associated with the
   * given key.
   *
   * @param entityId entity id.
   * @param newType new type that should be stored.
   * @return <code>True</code>, if successful. <code>False</code> otherwise.
   */
  def changeType(entityId: Long, newType: EntityType.Value): Boolean
}
