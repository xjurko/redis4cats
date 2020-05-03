/*
 * Copyright 2018-2020 ProfunKtor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.profunktor.redis4cats

import cats.effect._
import cats.syntax.functor._
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.log4cats._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import java.util.concurrent.{ Executors, TimeUnit }
import scala.concurrent.ExecutionContext

/**
  * Provides an instance of `Log` given an instance of `Logger`.
  *
  * For simplicity and re-usability in all the examples.
  * */
trait TxLoggerIOApp extends IOApp.WithContext {

  // Custom EC for Redis, especially for transactional commands that need
  // to be single-threaded.
  override def executionContextResource: Resource[SyncIO, ExecutionContext] =
    Resource
      .make(SyncIO(Executors.newFixedThreadPool(2)))(pool =>
        SyncIO {
          pool.shutdown()
          pool.awaitTermination(10, TimeUnit.SECONDS)
          ()
        }
      )
      .map(ExecutionContext.fromExecutorService)

  def program(implicit log: Log[IO]): IO[Unit]

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

}
