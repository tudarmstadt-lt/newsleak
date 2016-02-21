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

package divid

import sbt._
import Keys._

object Dependencies {

  val Versions = Seq(
    scalaVersion := "2.11.7"
    // scalaTestVersion := ...
  )

  object Compile {
    // Compile

    val config = "com.typesafe" % "config" % "1.3.0" // ApacheV2
    val logging = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" // ApacheV2
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.6" // MIT
    val scopt = "com.github.scopt" %% "scopt" % "3.3.0" // MIT
    val playJson = "com.typesafe.play" %% "play-json" % "2.4.3" // ApacheV2
    val csv = "com.github.tototoshi" %% "scala-csv" % "1.2.2" // ApacheV2

    val ner = "org.scalanlp" %% "epic-ner-en-conll" % "2015.1.25" // ApacheV2
    val scalaNlp = "org.scalanlp" %% "epic" % "0.3" excludeAll (ExclusionRule(organization = "com.typesafe.scala-logging")) // ApacheV2

    val hikari = "com.zaxxer" % "HikariCP" % "2.4.2" // ApacheV2
    val h2database = "com.h2database" % "h2" % "1.4.190" // MPLV2 / EPL 1.0
    val postgresql = "org.postgresql" % "postgresql" % "9.4-1200-jdbc41" // PostgreSQL Licence

    val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % "2.3.5" // ApacheV2
    val scalikejdbcConfig = "org.scalikejdbc" %% "scalikejdbc-config" % "2.3.5" // ApacheV2

    val elastic4s = "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.2.0" // ApacheV2

    object Test {
      val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc-test" % "2.3.+" % "test" // ApacheV2
      val scalatest = "org.scalatest" %% "scalatest" % "2.2.5" % "test" // ApacheV2
      val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test" // BSD
    }
  }

  import Compile._

  val l = libraryDependencies

  // Projects
  val coreDeps = l ++= Seq(config, scopt, playJson, csv, logging, slf4jSimple, elastic4s,
    ner, scalaNlp, h2database, Test.scalikejdbc, Test.scalatest, Test.scalamock)

  val commonDeps = l ++= Seq(playJson, scalaNlp, elastic4s,
    postgresql, scalikejdbc, scalikejdbcConfig, h2database, logging, slf4jSimple,
    Test.scalikejdbc, Test.scalatest, Test.scalamock)
}
