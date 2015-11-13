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

import java.time.LocalDateTime

import scala.collection.immutable

import testFactories.FlatSpecWithCommonTraits

class DocumentTest extends FlatSpecWithCommonTraits {

  val metadata = immutable.Map(
    "Subject" -> ("Text", List("Subject of the document")),
    "Tags" -> ("Text", List("EFIS", "PBTS", "AR"))
  )

  val uut = Document(2, "This is a sample \n 12%&/", LocalDateTime.parse("2007-12-03T10:15:30"), metadata)

  behavior of "Document"

  it should "has correct id" in {
    assert(uut.id == 2)
  }

  it should "has correct content" in {
    assert(uut.content == "This is a sample \n 12%&/")
  }

  it should "has correct datetime" in {
    assert(uut.created == LocalDateTime.parse("2007-12-03T10:15:30"))
  }

  it should "contain correct number of metadata" in {
    assert(uut.metadata.size == 2)
  }
}
