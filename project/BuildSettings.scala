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

// SBT
import sbt._
import Keys._

// sbt-assembly
import sbtassembly._
import sbtassembly.AssemblyKeys._

// sbt-native-packager
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.SbtNativePackager.Docker
import com.typesafe.sbt.packager.docker.DockerVersion
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._

object BuildSettings {

  lazy val assemblySettings = Seq(
    assembly / assemblyJarName := moduleName.value + "-" + version.value + ".jar",
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                             => MergeStrategy.discard
      case x if x.endsWith("paginators-1.json")            => MergeStrategy.first
      case x if x.endsWith("examples-1.json")              => MergeStrategy.first
      case x if x.endsWith("waiters-2.json")               => MergeStrategy.first
      case x if x.endsWith("service-2.json")               => MergeStrategy.first
      case x if x.endsWith("native-image.properties")      => MergeStrategy.first
      case x if x.endsWith("customization.config")         => MergeStrategy.first
      case x if x.endsWith("reflection-config.json")       => MergeStrategy.first
      case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
      case x if x.endsWith("AUTHORS")                      => MergeStrategy.first
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

  lazy val dockerSettings = Seq(
    Docker / daemonUser := "daemon",
    Docker / daemonUserUid := None,
    Docker / defaultLinuxInstallLocation := "/opt/snowplow",
    Docker / maintainer := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>",
    dockerBaseImage := "adoptopenjdk:11-jre-hotspot-focal",
    dockerUpdateLatest := true,
    dockerUsername := Some("snowplow")
  )

  lazy val buildSettings: Seq[Setting[_]] = Seq[Setting[_]](
    initialCommands := "import com.snowplowanalytics.blob2stream._",
    addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.0").cross(CrossVersion.full))
  ) ++ assemblySettings ++ dockerSettings
}
