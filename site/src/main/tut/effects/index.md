---
layout: docs
title:  "Effect-based API"
number: 3
position: 2
---

# Effect-based API

The API that operates at the effect level `F[_]` on top of `cats-effect`.

- **[Connection API](./connection)**
- **[Geo API](./geo)**
- **[Hashes API](./hashes)**
- **[Lists API](./lists)**
- **[Server API](./server)**
- **[Sets API](./sets)**
- **[Sorted SetsAPI](./sortedsets)**
- **[Strings API](./strings)**

### Acquiring client and connection

For all the effect-based APIs the process of acquiring a client and a commands connection is via the `apply` method that returns a `Resource`:

```scala
def apply[F[_]](uri: RedisURI): Resource[F, RedisClient]
```

### Logger

In order to create a client and/or connection you must provide a `Log` instance that the library uses for internal logging. You could either create your own or use `log4cats` (recommended). `fs2-redis` can derive an instance of `Log[F]` if there is an instance of `Logger[F]` in scope, just need to add the extra dependency `fs2-redis-log4cats` and `import dev.profunktor.redis4cats.log4cats._`.

Take a look at the [examples](https://github.com/gvolpe/fs2-redis/blob/master/modules/examples/src/main/scala/dev.profunktor.redis4cats/LoggerIOApp.scala) to find out more.

### Establishing connection

Here's an example of acquiring a client and a connection to the `Strings API`:

```tut:book:silent
import cats.effect.{IO, Resource}
import cats.syntax.all._
import dev.profunktor.redis4cats.algebra.StringCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.{LiveRedisCodec, RedisCodec}
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import io.lettuce.core.{ RedisURI => JRedisURI }
import io.lettuce.core.codec.{RedisCodec => JRedisCodec, StringCodec => JStringCodec}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)
implicit val logger: Logger[IO] = Slf4jLogger.unsafeCreate[IO]

val stringCodec: RedisCodec[String, String] = LiveRedisCodec(JStringCodec.UTF8)

val commandsApi: Resource[IO, StringCommands[IO, String, String]] =
  for {
    uri    <- Resource.liftF(RedisURI.make[IO]("redis://localhost"))
    client <- RedisClient[IO](uri)
    redis  <- Redis[IO, String, String](client, stringCodec, uri)
  } yield redis
```

The only difference with other APIs will be the `Commands` type. For the `Strings API` is `StringCommands`, for `Sorted Sets API` is `SortedSetCommands` and so on. For a complete list please take a look at the
[algebras](https://github.com/gvolpe/fs2-redis/tree/master/modules/core/src/main/scala/dev.profunktor.redis4cats/algebra).

### Standalone, Sentinel or Cluster

You can connect in any of these modes by either using `JRedisURI.create` or `JRedisURI.Builder`. More information
[here](https://github.com/lettuce-io/lettuce-core/wiki/Redis-URI-and-connection-details).

### Cluster connection

The process looks mostly like standalone connection but with small differences:

```scala
val commandsApi: Resource[IO, StringCommands[IO, String, String]] =
  for {
    uri    <- Resource.liftF(RedisURI.make[IO]("redis://localhost:30001"))
    client <- RedisClusterClient[IO](uri)
    redis  <- Redis.cluster[IO, String, String](client, stringCodec, uri)
  } yield redis
```

### Master / Slave connection

The process is a bit different. First of all, you don't need to create a `RedisClient`, it'll be created for you. All you need is `RedisMasterSlave` that exposes in a similar way one method `apply` that returns a `Resource`.

```scala
def apply[F[_], K, V](codec: RedisCodec[K, V], uris: JRedisURI*)(
  readFrom: Option[JReadFrom] = None): Resource[F, RedisMasterSlaveConnection[K, V]]
```

#### Example using the Strings API

```tut:book:silent
import cats.effect.{IO, Resource}
import cats.syntax.all._
import dev.profunktor.redis4cats.algebra.StringCommands
import dev.profunktor.redis4cats.connection.RedisMasterSlave
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.domain.RedisMasterSlaveConnection
import io.lettuce.core.{ReadFrom => JReadFrom, RedisURI => JRedisURI}
import io.lettuce.core.codec.{RedisCodec => JRedisCodec, StringCodec => JStringCodec}

val stringCodec: RedisCodec[String, String] = LiveRedisCodec(JStringCodec.UTF8)

val connection: Resource[IO, RedisMasterSlaveConnection[String, String]] =
  Resource.liftF(RedisURI.make[IO]("redis://localhost")).flatMap { uri =>
    RedisMasterSlave[IO, String, String](stringCodec, uri)(Some(JReadFrom.MASTER_PREFERRED))
  }

connection.use { conn =>
  Redis.masterSlave[IO, String, String](conn).flatMap { cmd =>
    IO.unit  // do something
  }
}
```

Find more information [here](https://github.com/lettuce-io/lettuce-core/wiki/Master-Slave#examples).