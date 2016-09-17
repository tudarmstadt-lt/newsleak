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

import model.Tag
import model.queryable.TagQueryable
import utils.DBSettings

// scalastyle:off
import scalikejdbc._
// scalastyle:on

class TagQueryableImpl(conn: () => NamedDB) extends TagQueryable with DBSettings {

  def connector: NamedDB = conn()

  override def getById(tagId: Long): Option[Tag] = connector.readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.id = ${tagId}
    """.map(Tag(_)).single().apply()
  }

  override def getByLabel(label: String): List[Tag] = connector.readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE l.label = ${label}
    """.map(Tag(_)).list().apply()
  }

  override def getByDocumentId(docId: Long): List[Tag] = connector.readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.documentid = ${docId}
    """.map(Tag(_)).list().apply()
  }

  override def getDistinctLabels(): List[String] = connector.readOnly { implicit session =>
    sql"SELECT label FROM labels".map(_.string("label")).list().apply()
  }

  override def add(docId: Long, label: String): Tag = connector.localTx { implicit session =>
    val labelId = getOrCreateLabel(label)

    val tagOpt = getByValues(docId, labelId)
    tagOpt.getOrElse {
      val tagId = sql"INSERT INTO tags (documentid, labelid) VALUES (${docId}, ${labelId})".updateAndReturnGeneratedKey().apply()
      Tag(tagId, docId, label)
    }
  }

  override def delete(tagId: Long): Boolean = connector.autoCommit { implicit session =>
    val tagOpt = getById(tagId)
    tagOpt.exists { t =>
      val count = sql"DELETE FROM tags WHERE id = ${t.id}".update().apply()

      // Check if there are remaining tags that reference the label
      val otherTags = getByLabel(t.label)
      if (otherTags.isEmpty) {
        // We need to remove those labels
        sql"DELETE FROM labels WHERE label = ${t.label}".update().apply()
      }
      count == 1
    }
  }

  private def getOrCreateLabel(label: String)(implicit session: DBSession): Long = {
    val idOpt = sql"SELECT id FROM labels WHERE label = ${label}".map(_.long("id")).single().apply()

    idOpt getOrElse {
      sql"INSERT INTO labels (label) VALUES (${label})".updateAndReturnGeneratedKey().apply()
    }
  }

  private def getByValues(docId: Long, labelId: Long)(implicit session: DBSession): Option[Tag] = {
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.documentid = ${docId} AND t.labelid = ${labelId}
    """.map(Tag(_)).single().apply()
  }
}
