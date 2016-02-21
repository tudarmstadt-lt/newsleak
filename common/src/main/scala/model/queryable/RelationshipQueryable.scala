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

import model.Relationship

trait RelationshipQueryable {

  /**
   * Returns all [[Relationship]]s available in the collection.
   * @return
   */
  def getRelationships(): List[Relationship]

  /**
   * Returns all [[Relationship]]s where the given [[model.Entity]] participates.
   * @param entityId the id of the [[model.Entity]] to search for.
   * @return List[Relationship]
   */
  def getRelationshipByEntity(entityId: Long): List[Relationship]

  /**
   * Returns all [[Relationship]]s per document where an [[model.Entity]] participates.
   * @param entityId the [[model.Entity]] id
   * @param docId the [[model.Document]] id
   * @return
   */
  def getRelationshipByEntity(entityId: Long, docId: Long): List[Relationship]

  /**
   * Returns all [[Relationship]]s in the given document.
   *
   * @param docId the document id
   * @return List[[Relationship]]
   */
  def getRelationshipByDocument(docId: Long): List[Relationship]
}
