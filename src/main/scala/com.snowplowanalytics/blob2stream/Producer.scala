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

import java.util.UUID
import java.util.concurrent.Executors

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import cats.implicits.catsSyntaxOptionId
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Async, Resource, Sync}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration}

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import com.permutive.pubsub.producer.Model.{ProjectId, Topic}
import com.permutive.pubsub.producer.grpc.{GooglePubsubProducer, PubsubProducerConfig}

import com.snowplowanalytics.blob2stream.Job.Message

trait Producer[F[_]] {
  def send(message: Message): F[Unit]
}

object Producer {
  def pubsub[F[_]: Async](projectId: ProjectId, topic: Topic): Resource[F, Producer[F]] =
    GooglePubsubProducer
      .of[F, Message](
        projectId,
        topic,
        config = PubsubProducerConfig[F](
          batchSize = 1000L,
          requestByteThreshold = 10000000L.some,
          delayThreshold = 100.millis,
          onFailedTerminate = e => Sync[F].delay(println(s"Got error $e"))
        )
      )
      .map { pubSub =>
        new Producer[F] {
          def send(message: Message): F[Unit] = pubSub.produce(message).void
        }
      }

  def kinesis[F[_]: Async](stream: String, region: String): Resource[F, Producer[F]] = {
    val config = for {
      credentialsChain <- Sync[F].delay(new DefaultAWSCredentialsProviderChain())
      config <- Sync[F].delay(
        new KinesisProducerConfiguration().setCredentialsProvider(credentialsChain).setRegion(region)
      )
      poolSize <- Sync[F].delay(Runtime.getRuntime.availableProcessors() * 3)
    } yield (poolSize, config)

    for {
      init <- Resource.liftF(config)
      (poolSize, config) = init
      ec <- Resource
        .make(Sync[F].delay(Executors.newFixedThreadPool(poolSize)))(ex => Sync[F].delay(ex.shutdown()))
        .map(es => ExecutionContext.fromExecutorService(es))
      producer <- Resource.make(Sync[F].delay(new KinesisProducer(config)))(p => Sync[F].delay(p.destroy()))
    } yield new Producer[F] {
      def send(message: Message): F[Unit] = {
        val result = for {
          partitionKey <- Sync[F].delay(UUID.randomUUID())
          future       <- Sync[F].delay(producer.addUserRecord(stream, partitionKey.toString, message.toByteBuffer))
          result       <- toAsync(ec, future)
        } yield result

        result.void
      }
    }
  }

  private def toAsync[F[_]: Async, T](es: ExecutionContext, future: ListenableFuture[T]): F[T] =
    Async[F].async { cb =>
      Futures.addCallback(
        future,
        new FutureCallback[T] {
          def onFailure(t: Throwable): Unit = cb(Left(t))

          def onSuccess(result: T): Unit = cb(Right(result))
        },
        (command: Runnable) => es.execute(command)
      )
    }
}
