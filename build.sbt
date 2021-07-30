/*
 * Copyright (c) 2021 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("fmt", "; scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "; scalafmtCheckAll; scalafmtSbtCheck")

lazy val blob2stream = project
  .in(file("."))
  .settings(
    name := "blob2stream",
    version := "0.1.0-rc4",
    organization := "com.snowplowanalytics",
    scalaVersion := "2.13.6",
    javacOptions := Seq("-source", "11", "-target", "11"),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
  )
  .settings(BuildSettings.buildSettings)
  .settings(libraryDependencies ++= Dependencies.all)
  .enablePlugins(BuildInfoPlugin, DockerPlugin, JavaAppPackaging)
