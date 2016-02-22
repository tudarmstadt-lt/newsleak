# NEW/S/LEAK
[![Build Status](https://travis-ci.org/tudarmstadt-lt/DIVID-DJ.svg?branch=master)](https://travis-ci.org/tudarmstadt-lt/DIVID-DJ/)
[![Coverage Status](https://coveralls.io/repos/tudarmstadt-lt/DIVID-DJ/badge.svg?branch=master&service=github)](https://coveralls.io/github/tudarmstadt-lt/DIVID-DJ?branch=master)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Project Licence](https://img.shields.io/badge/licence-AGPL-blue.svg)](#license)


Science and Data-Driven Journalism: Data Extraction and Interactive Visualization of Unexplored Textual Datasets for Investigative Data-Driven Journalism (DIVID-DJ)

* Web site: [Project Description](https://www.lt.informatik.tu-darmstadt.de/de/research/divid-djdata-extraction-and-interactive-visualization-of-unexplored-textual-datasets-for-investigative-data-driven-journalism/)
* Web site: [Project scala documentation](http://tudarmstadt-lt.github.io/newsleak)

![newsleak](http://maggie.lt.informatik.tu-darmstadt.de/divid/wp-content/uploads/2016/02/cropped-logo-draft-2.png)

## 1. Build Instructions

The DIVID-DJ project consists of two dependent sub-projects (common and core) linked via a multi-sbt-project build. Run `sbt clean compile assembly` to compile and assembly all sub-projects. In case the build is successful, the jar files can be found in the projects `target/scala-2.11/` folder.

Make sure you have the build tool `sbt` installed. It can be downloaded [here](http://www.scala-sbt.org/). To change the build process or add/remove dependencies have a look in the `project` folder.


## Want to help?

Want to find a bug, contribute some code, or improve documentation? Read up on our guidelines for [contributing](https://github.com/tudarmstadt-lt/DIVID-DJ/CONTRIBUTING.md) and then check out one of our issues.

## License

```
Copyright (C) 2015  Language Technology Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
