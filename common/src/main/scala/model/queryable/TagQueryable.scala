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

import model.Tag

trait TagQueryable {

  /**
   * Returns the Tag with the given id
   *
   * @param tagId tag id to search for
   * @return Some(tag) if a tag with the given id exists.
   *         None otherwise.
   */
  def getById(tagId: Long): Option[Tag]

  /**
   * Returns all tags with the given label in all
   * available documents.
   *
   * @param label label to search for
   * @return list of tags with the same label
   */
  def getByLabel(label: String): List[Tag]

  /**
   * Returns all tags that are associated with the given
   * document.
   *
   * @param docId document id
   * @return list of tags that are associated with the given
   *         document.
   */
  def getByDocumentId(docId: Long): List[Tag]

  /**
   * Returns all distinct available labels over all
   * existing tags.
   *
   * Note: Labels group tags implicitly. This method can
   * therefore be used to retrieve all groups of tags.
   * To further retrieve documents that are associated
   * with a label group, call [[TagQueryable.getByLabel]].
   *
   * @return distinct labels over all existing tags.
   */
  def getDistinctLabels(): List[String]

  /**
   * Adds a new tag to the document. It will return the existing
   * tag, if the document is already tagged with the given one.
   *
   * @param docId document to be tagged
   * @param label tag label
   * @return new created or already existing tag
   */
  def add(docId: Long, label: String): Tag

  /**
   * Removes the the tag from the database. Issues a real
   * delete!
   *
   * @param tagId associated id for the tag to delete
   * @return <code>True</code> if successful. </code>False</code>,
   *         otherwise.
   */
  def delete(tagId: Long): Boolean
}
