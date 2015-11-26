/*
 * Copyright (C) 2015  Language Technology Group
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package reader

import java.io.IOException
import java.nio.file.Paths

import org.scalatest.{WordSpec, Matchers}

class CSVCorpusReaderTest extends WordSpec with Matchers {

  val directory = Paths.get("core/src/test/resources/documents/")

  trait ReaderWithOneDocumentMetadata {
    val reader = new CSVCorpusReader(directory.resolve("1document_metadata"))
    val document = reader.documents.head
  }

  trait ReaderWithThreeDocumentsMetaData {
    val reader = new CSVCorpusReader(directory.resolve("3document_metadata"))
  }

  trait ReaderWithOneDocumentNoMetaData {
    val reader = new CSVCorpusReader(directory.resolve("1document_no_metadata"))
    val document = reader.documents.head
  }

  trait ReaderWithOneDocumentIllFormed {
    val reader = new CSVCorpusReader(directory.resolve("1document_illformed"))
  }

  trait ReaderWithOneDocumentIllFormedTypes {
    val reader = new CSVCorpusReader(directory.resolve("1document_illformed_types"))
  }

  trait ReaderWithOneDocumentIllFormedDate {
    val reader = new CSVCorpusReader(directory.resolve("1document_illformed_date"))
  }

  trait ReaderWithOneDocumentMissingDate {
    val reader = new CSVCorpusReader(directory.resolve("1document_missing_date"))
  }

  "CSVCorpusReader" should {
    "provide the correct number of documents" in {
      new ReaderWithThreeDocumentsMetaData {
        reader.documents.size should be(3)
      }
    }
  }

  "CSVCorpusReader" should {
    "handle documents without metadata correct" in {
      new ReaderWithOneDocumentNoMetaData {
        document.metadata.size should be(0)
        reader.documents.size should be(1)
      }
    }
  }

  "CSVCorpusReader" should {
    "load multiple meta data occurrences" in {
      new ReaderWithOneDocumentMetadata {
        val (_, values) = document.metadata("Tags")
        values.size should be(3)
      }
    }
  }

  "CSVCorpusReader" should {
    "should assign correct data type to meta data" in {
      new ReaderWithOneDocumentMetadata {
        val (datatype, _) = document.metadata("Tags")
        datatype should be("Text")
      }
    }
  }

  "CSVCorpusReader" should {
    "throw an error if meta data fields are no multiple of three" in {
      new ReaderWithOneDocumentIllFormed {
        intercept[IOException] {
          reader.documents.head
        }
      }
    }
  }

  "CSVCorpusReader" should {
    "throw an error if date is missing" in {
      new ReaderWithOneDocumentMissingDate {
        intercept[IOException] {
          reader.documents.head
        }
      }
    }
  }

  "CSVCorpusReader" should {
    "throw an error if date is ill-formed" in {
      new ReaderWithOneDocumentIllFormedDate {
        intercept[IOException] {
          reader.documents.head
        }
      }
    }
  }

  "CSVCorpusReader" should {
    "throw an error if values with same key have different types" in {
      new ReaderWithOneDocumentIllFormedTypes {
        intercept[IOException] {
          reader.documents.head.metadata
        }
      }
    }
  }
}
