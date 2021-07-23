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

logLevel := Level.Warn

addSbtPlugin("com.eed3si9n"              % "sbt-assembly"        % "0.15.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"        % "0.1.16")
addSbtPlugin("com.eed3si9n"              % "sbt-buildinfo"       % "0.10.0")
addSbtPlugin("com.github.sbt"            % "sbt-native-packager" % "1.9.2")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"        % "2.4.3")
