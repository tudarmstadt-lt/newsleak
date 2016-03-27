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

import scala.collection.mutable

/**
 * Object representation for relationships.
 *
 * @param id unique id and primary key of the relationship.
 * @param e1 first entity. The order of entities is determined alphabetically. Especially, if (e1, e2) is a
 *           relationship, (e2, e1) is not.
 * @param e2 second entity.
 * @param frequency frequency of the relationship (i.e. co-occurrence) in the underlying data.
 * @param docIds list of document ids that represent the occurrence of the relation in the documents.
 */
case class Relationship(var id: Option[Int] = None, e1: Int, e2: Int, var occurrence: mutable.Map[Int, Int])
