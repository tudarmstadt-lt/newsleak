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

    val config   = "com.typesafe" % "config" % "1.3.0"                        // ApacheV2
    val logging  = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"  // ApacheV2
    val logback  = "ch.qos.logback" % "logback-classic" % "1.1.3"             // EPL 1.0 / LGPL 2.1
    val scopt    = "com.github.scopt" %% "scopt" % "3.3.0"                    // MIT
    val playJson = "com.typesafe.play" %% "play-json" % "2.4.3"               // ApacheV2
    val csv      = "com.github.tototoshi" %% "scala-csv" % "1.2.2"            // ApacheV2

    val jungApi     = "net.sf.jung" % "jung-api" % "2.0.1"           // BSD
    val jungGraph   = "net.sf.jung" % "jung-graph-impl" % "2.0.1"    // BSD
    val jungAlgo    = "net.sf.jung" % "jung-algorithms" % "2.0.1"    // BSD

    val mysql         = "mysql" % "mysql-connector-java" % "5.1.37"  // GPL
    val hikari        = "com.zaxxer" % "HikariCP" % "2.4.2"          // ApacheV2
    val h2database    = "com.h2database" % "h2" % "1.4.190"          // MPLV2 / EPL 1.0
    val scalalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % "2.2.+" // ApacheV2


    object Test {
      val scalalikejdbc = "org.scalikejdbc" %% "scalikejdbc-test" % "2.2.+" % "test"          // ApacheV2
      val scalatest     = "org.scalatest" %% "scalatest" % "2.2.5" % "test"                   // ApacheV2
      val scalamock     = "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test" // BSD
    }
  }

  import Compile._

  val l = libraryDependencies

  // Projects
  val coreDeps = l ++= Seq(config, scopt, playJson, csv, logging, logback,
        jungApi, jungGraph, jungAlgo, mysql, hikari, h2database,
        scalalikejdbc, Test.scalalikejdbc, Test.scalatest, Test.scalamock)

  val commonDeps = l ++= Seq(playJson, jungApi, jungGraph, jungAlgo, mysql, hikari, h2database,
        logging, logback, scalalikejdbc, Test.scalalikejdbc, Test.scalatest, Test.scalamock)
}
