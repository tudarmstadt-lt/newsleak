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

package utils

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import scalikejdbc.{ConnectionPool, GlobalSettings, NamedDB, SQLFormatterSettings}
import scalikejdbc.config.{DBs, NoEnvPrefix, StandardTypesafeConfig, TypesafeConfigReader}

/**
 * Mix in this trait if you are going to use database interactions.
 * It will initialize the connection pool, if the pool isn't already
 * initialized. Also make sure that your database connection is configured
 * in /conf/application.conf properly. See example below:
 *
 * db.default.driver=org.postgresql.Driver
 * db.default.url="jdbc:postgresql://127.0.0.1/databasename"
 * db.default.username=""
 * db.default.password=""
 *
 * For more information refer to: http://scalikejdbc.org/documentation/configuration.html
 */
trait DBSettings {
  DBService.initialize()
}

object DBService {

  private var isInitialized = false

  private var activeDBName = ConnectionPool.DEFAULT_NAME
  def connector: NamedDB = NamedDB(activeDBName)

  def initialize(): Unit = this.synchronized {
    if (!isInitialized) {
      MyDBs.setupAll()
      GlobalSettings.loggingSQLErrors = true
      GlobalSettings.sqlFormatter = SQLFormatterSettings("utils.HibernateSQLFormatter")
      isInitialized = true
    }
  }

  def changeDB(dbName: String): Unit = activeDBName = Symbol(dbName)
}

/**
 * Handles the initialization of the connections defined in the
 * conf/application.conf file.
 */
object MyDBs extends DBs
    with TypesafeConfigReader
    with StandardTypesafeConfig
    with NoEnvPrefix {
  override lazy val config: Config = ConfigFactory.parseFile(new File("conf/application.conf"))
}
