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

resolvers += "Sonatype-releases"   at "http://oss.sonatype.org/content/repositories/releases"
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

// Exclude the old version of scalariform and manually use the new one
// See: https://github.com/scalastyle/scalastyle/issues/156
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0" excludeAll(
  ExclusionRule(organization = "com.danieltrinh")))

libraryDependencies += "org.scalariform" %% "scalariform" % "0.1.7"

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.3")



