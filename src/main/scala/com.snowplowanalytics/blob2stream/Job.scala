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
import java.nio.ByteBuffer

import cats.effect.{Async, Blocker, Concurrent, ConcurrentEffect, ContextShift, Resource, Sync}
import cats.implicits._

import fs2.{Pipe, Stream}
import fs2.text.{lines, utf8Decode}
import fs2.compression.gunzip

import blobstore.{Path, Store}
import blobstore.gcs.GcsStore
import blobstore.s3.S3Store

import com.google.cloud.storage.StorageOptions

import software.amazon.awssdk.services.s3.S3AsyncClient

import com.permutive.pubsub.producer.encoder.MessageEncoder

import com.snowplowanalytics.blob2stream.Main.Output

object Job {

  sealed trait Message {
    def toByteBuffer: ByteBuffer =
      this match {
        case Message.Binary(data) => ByteBuffer.wrap(data)
        case Message.Text(data)   => ByteBuffer.wrap(data.getBytes)
      }
  }
  object Message {
    case class Binary(data: Array[Byte]) extends Message
    case class Text(data: String) extends Message
  }

  def getStore[F[+_]: ConcurrentEffect: ContextShift](blocker: Blocker, uri: URI): F[Store[F]] =
    Option(uri.getScheme) match {
      case Some("gs") =>
        Sync[F].delay(StorageOptions.getDefaultInstance.getService).map { storage =>
          GcsStore(storage, blocker, List.empty)
        }
      case Some("s3") =>
        Sync[F].delay(S3AsyncClient.builder().build()).flatMap { client =>
          S3Store[F](client)
        }
      case _ =>
        Sync[F].raiseError(new IllegalArgumentException(s"Cannot build a client for $uri"))
    }

  implicit val encoder: MessageEncoder[Message] = {
    case Message.Binary(data) => data.asRight
    case Message.Text(data)   => data.getBytes.asRight
  }

  def getOutput[F[_]: Async](output: Output): Resource[F, Producer[F]] =
    output match {
      case Output.PubSubTopic(projectId, topic) =>
        Producer.pubsub[F](projectId, topic)
      case Output.KinesisStream(name) =>
        Producer.kinesis[F](name, "eu-central-1")
    }

  def list[F[_]](store: Store[F])(uri: URI): Stream[F, Path] =
    store.list(Path(uri.toString), recursive = true)

  def text[F[_]: Concurrent](ungzip: Boolean)(store: Store[F], maxConcurrency: Int): Pipe[F, Path, Message] = {
    val pipe: Pipe[F, Byte, String] = in => {
      val bytes = if (ungzip) in.through(gunzip[F]()).flatMap(_.content) else in
      bytes.through(utf8Decode).through(lines)
    }

    _.map(path => store.get(path, 4096).through(pipe))
      .parJoin(maxConcurrency)
      .filter(_.nonEmpty)
      .map(Message.Text.apply)
  }

  def binary[F[_]: Concurrent](store: Store[F], maxConcurrency: Int): Pipe[F, Path, Message] =
    _.parEvalMapUnordered(maxConcurrency)(path => store.get(path, 4096).compile.to(Array).map(Message.Binary.apply))
}
