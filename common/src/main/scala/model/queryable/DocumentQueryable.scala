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

import model.{TimeExpression, Document}
import org.joda.time.{LocalDateTime, LocalDate}

trait DocumentQueryable {

  /**
   * Returns all [[model.Document]] Ids available in the collection.
   * @return  List[Long]
   */
  def getIds(): List[Long]

  /**
   * Returns the [[model.Document]] associated with the given id
   * @param id the document id
   * @return Some([[model.Document]]) if a document with the given id
   *         exists. None otherwise.
   */
  def getById(id: Long): Option[Document]

  /**
   * Returns a list of [[model.Document]] containing documents that are
   * created on the given date.
   * @param date date to search for
   * @return List of [[model.Document]]
   */
  def getByDate(date: LocalDate): List[Document]

  /**
   * Returns list of [[model.Document]] that contain the given [[model.Entity.id]].
   * @param id the [[model.Entity]] id
   * @return List of [[model.Document]]
   */
  def getByEntityId(id: Long): List[Document]

  /**
   * Returns [[model.Document]]s that contain the given [[model.Relationship]].
   * @param id relationship id
   * @return List[Document]
   */
  def getByRelationshipId(id: Long): List[Document]

  /**
   * Returns a list of document ids where each document in the result list
   * contains the given [[model.Relationship]].
   * @param id relationship id
   * @return List[Long]
   */
  def getIdsByRelationshipId(id: Long): List[Long]

  /**
   * Returns a list of [[model.TimeExpression]] that are contained in this
   * document identified by the given document id.
   * @param docId document id
   * @return List of [[model.TimeExpression]]
   */
  def getTimeExpressions(docId: Long): List[TimeExpression]

  /**
   * Returns a list of tuple, where each tuple (key, type) represents
   * the available metadata key associated with its type in the <b>whole</b> collection.
   * Example: List(("Subject", "TEXT", "Position":"GEO", ...)
   *
   * @return List[(String, String)]
   */
  def getMetadataKeysAndTypes(): List[(String, String)]

  /**
   * Returns all instances for a given metadata key.
   *
   * Example:
   * Assume the only signers in the whole document collections are "Clinton"
   * and "Merkel".
   *
   * getMetadataKeyInstances("SignedBy") returns List("Clinton","Merkel")
   * @return
   */
  def getMetadataKeyInstances(key: String): List[String]

  /**
   * Get list of metadata key and their types that is available for the given document.
   * Example: List((SignedBy,Text), (Header,Text))
   *
   * @param id: the id of the [[model.Document]].
   * @return List of metadata keys with their types present for the document.
   */
  def getMetadataType(id: Long): List[(String, String)]

  /**
   * Returns metadata values for a given metadata key for a given document.
   * Example: [`IR`, `MARR`, `MASS`] for the key `Tags`.
   *
   * @param docId  document id.
   * @param key name of the metadata key to search for e.g `Tags`.
   * @return List of metatadata values for the given key and document id.
   */
  def getMetadataValueByDocumentId(docId: Long, key: String): List[String]

  /**
   * Returns a list of tuple (key, value) for a given document id.
   * Example: List(SignedBy,HECK), (Tags,IR), (Tags,MASS), ...)
   *
   * @param docId document id to get list of metadata key, value tuple for.
   * @return  List[(String, String)].
   */
  def getMetadataKeyValueByDocumentId(docId: Long): List[(String, String)]

  def getMetadataForDocuments(docIds: List[Long], fields: List[String]): List[(Long, String, String)]
}
