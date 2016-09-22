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

import model.KeyTerm
import model.queryable.KeyTermQueryable
import utils.DBSettings

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class KeyTermQueryableImpl(conn: () => NamedDB) extends KeyTermQueryable with DBSettings {

  def connector: NamedDB = conn()
  protected val document = new DocumentQueryableImpl(conn)

  override def getDocumentKeyTerms(docId: Long, limit: Option[Int] = None): List[KeyTerm] = connector.readOnly { implicit session =>
    SQL("""SELECT term, frequency
          FROM terms
          WHERE docid = {docId}
          %s
      """.format(if (limit.isDefined) "LIMIT " + limit.get else "")).bindByName('docId -> docId).map(KeyTerm(_)).list.apply()
  }

  override def getRelationshipKeyTerms(relId: Long, limit: Option[Int] = None): List[KeyTerm] = connector.readOnly { implicit session =>
    val docIds = document.getIdsByRelationshipId(relId)
    val terms = sql"""
          SELECT term
          FROM terms
          WHERE docid IN (${docIds.mkString(",")})
      """.map(_.string("term")).list.apply()

    val res = aggregateKeyTerms(terms)
    if (limit.isDefined) res.take(limit.get) else res
  }

  private def aggregateKeyTerms(terms: List[String]): List[KeyTerm] = {
    val termsToCount = terms.groupBy(identity).map {
      case (term, counts) =>
        KeyTerm(term, counts.length)
    }.toList
    termsToCount.sortWith { case (KeyTerm(_, c1), KeyTerm(_, c2)) => c1 >= c2 }
  }
}

