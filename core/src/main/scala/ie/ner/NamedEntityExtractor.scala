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

package ie.ner

import model.{Document, EntityType}

/**
 * Common trait for different named entity extraction algorithms.
 */
trait NamedEntityExtractor {

  /**
   * Returns named entities with their types [[EntityType.Value]] from a given [[model.Document]].
   *
   * @param doc document that should be used to extract named entities from.
   * @return a list that contains tuple of the following form (named entity name, ne-type).
   *         If the `doc` contains no named entities the list will be empty.
   */
  def extractNamedEntities(doc: Document): List[(String, EntityType.Value)]
}
