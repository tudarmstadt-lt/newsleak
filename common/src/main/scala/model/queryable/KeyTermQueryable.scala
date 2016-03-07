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

import model.KeyTerm

trait KeyTermQueryable {

  /**
   * Returns List of all <b>important</b> [[KeyTerm]] for a given document.
   *
   * @param docId the document id
   * @param limit limits the number of returned [[KeyTerm]]
   * @return  list of [[KeyTerm]]
   */
  def getDocumentKeyTerms(docId: Long, limit: Option[Int] = None): List[KeyTerm]

  /**
   * Returns ordered list of <b>important</b> [[KeyTerm]] for the given relationship.
   * [[KeyTerm.score]] corresponds to the occurrence of this term as important term
   * over multiple documents i.e how many documents mark this term as important.
   * The list is ordered by term importance.
   *
   * @param relId relationship id
   * @param limit limits the number of returned important terms.
   * @return list of important terms describing the relationship
   */
  def getRelationshipKeyTerms(relId: Long, limit: Option[Int] = None): List[KeyTerm]
}

