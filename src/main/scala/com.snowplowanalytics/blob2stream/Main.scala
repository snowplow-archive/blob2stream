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

package com.snowplowanalytics.blob2stream

import java.net.URI

import fs2.{Pipe, Stream}

import blobstore.{Path, Store}

import cats.implicits._
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}

import com.monovore.decline.{Command, Opts}

import com.permutive.pubsub.producer.Model.{ProjectId, Topic}

object Main extends IOApp {

  val input = Opts.option[URI]("input", "GCS or S3 Bucket input (full path)", "i").mapValidated { uri =>
    Option(uri.getScheme) match {
      case Some("gs" | "s3") => uri.validNel
      case _ => "Input URI should have (gs|s3):// protocol".invalidNel
    }
  }

  val output = Opts.option[String]("output", "PubSub topic or Kinesis Stream for output", "o").mapValidated { topic =>
    topic.split("/").toList match {
      case List("projects", project, "topics", topic) => Output.PubSubTopic(ProjectId(project), Topic(topic)).validNel
      case List(kinesisStream)                        => Output.KinesisStream(kinesisStream).validNel
      case _                                          => s"s$topic is invalid topic representation".invalidNel
    }
  }

  val maxConcurrency = Opts.option[Int]("maxConcurrency", "Maximum concurrency").withDefault(256)

  val limit = Opts.option[Int]("limit", "Amount of records to submit").orNone
  val region =
    Opts.option[String]("region", "Region to use for Kinesis (AWS only). Overwrites AWS_REGION env var,").orNone

  val binary = Opts.flag("binary", "Data is binary, not text").orFalse
  val ungzip = Opts.flag("ungzip", "Ungzip data on-fly").orFalse

  val parser =
    Command("blob2stream", "A job to send data from blob storage to stream")(
      (input, output, binary, limit, maxConcurrency, ungzip, region).mapN { (i, o, b, l, mc, u, r) =>
        Config(i, o, b, l, mc, u, r)
      }
    )

  sealed trait Output

  object Output {
    case class PubSubTopic(projectId: ProjectId, topic: Topic) extends Output

    case class KinesisStream(name: String) extends Output
  }

  case class Config(
    input: URI,
    output: Output,
    binary: Boolean,
    limit: Option[Int],
    mc: Int,
    ungzip: Boolean,
    region: Option[String]
  )

  def run(args: List[String]): IO[ExitCode] =
    parser.parse(args) match {
      case Right(Config(input, output, binary, limit, maxConcurrency, ungzip, region)) =>
        val pipe: (Store[IO], Int) => Pipe[IO, Path, Job.Message] = if (binary) Job.binary[IO] else Job.text[IO](ungzip)
        val limitPipe = limit match {
          case Some(value) => (in: Stream[IO, Job.Message]) => in.take(value.toLong)
          case None        => (in: Stream[IO, Job.Message]) => in
        }
        val resources = for {
          envRegion <- Resource.liftF(getRegion(region))
          blocker   <- Blocker[IO]
          producer  <- Job.getOutput[IO](output, envRegion)
        } yield (blocker, producer)
        resources.use { case (blocker, producer) =>
          for {
            store <- Job.getStore[IO](blocker, input)
            _ <- Job
              .list[IO](store)(input)
              .through(pipe(store, maxConcurrency))
              .through(limitPipe)
              .parEvalMapUnordered(maxConcurrency)(producer.send)
              .compile
              .drain
          } yield ExitCode.Success
        }
      case Left(error) => IO.delay(println(s"Error! ${error.show}")).as(ExitCode.Error)
    }

  def getRegion(fromCli: Option[String]): IO[Option[String]] =
    IO.pure(fromCli).flatMap {
      case Some(region) => IO.pure(Some(region))
      case None         => IO.delay(sys.env.get("AWS_REGION"))
    }
}
