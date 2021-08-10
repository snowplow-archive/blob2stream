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

import sbt._

object Dependencies {

  object V {
    // Scala
    val decline      = "1.3.0"
    val fs2PubSub    = "0.16.1"
    val fs2Blobstore = "0.7.3"
    val fs2Aws       = "3.0.10"
    val slf4j        = "1.7.30"
    val googleApis   = "1.118.1" // Override transitive dependency of fs2-blobstore
    // Scala (test only)
    val specs2     = "4.10.5"
    val scalaCheck = "1.15.1"
  }

  // Scala
  val all = List(
    "com.monovore"             %% "decline"                % V.decline,
    "com.permutive"            %% "fs2-google-pubsub-grpc" % V.fs2PubSub,
    "com.github.fs2-blobstore" %% "gcs"                    % V.fs2Blobstore,
    "com.github.fs2-blobstore" %% "s3"                     % V.fs2Blobstore,
    "com.google.cloud"          % "google-cloud-storage"   % V.googleApis,
    "io.laserdisc"             %% "fs2-aws"                % V.fs2Aws,
    "org.slf4j"                 % "slf4j-simple"           % V.slf4j,
    "org.specs2"               %% "specs2-core"            % V.specs2     % Test,
    "org.scalacheck"           %% "scalacheck"             % V.scalaCheck % Test
  )
}
