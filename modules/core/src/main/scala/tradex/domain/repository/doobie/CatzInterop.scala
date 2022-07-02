package tradex.domain
package repository.doobie

import zio._
import zio.interop.catz._
import doobie.hikari._

import tradex.domain.config._
import scala.concurrent.ExecutionContextExecutor
import cats.effect.kernel.Resource

trait CatzInterop {
  import zio.interop.catz.implicits._

  implicit val zioRuntime = zio.Runtime.default.unsafe
  implicit val ec: ExecutionContextExecutor =
    scala.concurrent.ExecutionContext.global

  implicit val dispatcher: cats.effect.std.Dispatcher[zio.Task] =
    Unsafe
      .unsafe(implicit u =>
        zioRuntime
          .run(
            cats.effect.std
              .Dispatcher[zio.Task]
              .allocated
          )
          .getOrThrowFiberFailure()
      )
      ._1

  def mkTransactor(
      cfg: DBConfig
  ): Task[Resource[Task, HikariTransactor[Task]]] =
    ZIO.attemptBlocking(
      HikariTransactor
        .newHikariTransactor[Task](
          cfg.driver,
          cfg.url,
          cfg.user,
          cfg.password,
          ec
        )
    )
}
