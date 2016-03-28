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

package testFactories

import org.scalatest.{BeforeAndAfterAll, FlatSpec}
// scalastyle:off
import scalikejdbc._
// scalastyle:on

/**
 * Mix in this trait if you want to drop all tables including their
 * content after all tests are executed. Autoincrement columns
 * will also be reset.
 */
trait DatabaseRollback extends FlatSpec with BeforeAndAfterAll {

  // Needs to be overwritten with the actual test database
  def testDatabase: NamedDB

  override def afterAll(): Unit = {
    testDatabase.localTx { implicit session =>
      sql"DROP ALL OBJECTS".update.apply()
    }
  }
}
