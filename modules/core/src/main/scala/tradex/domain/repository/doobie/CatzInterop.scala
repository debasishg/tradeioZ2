package tradex.domain
package repository.doobie

import zio._
import zio.interop.catz._
import doobie.hikari._

import config._

trait CatzInterop {
  import zio.interop.catz.implicits._

  implicit val zioRuntime: zio.Runtime[zio.ZEnv] = zio.Runtime.default

  implicit val dispatcher: cats.effect.std.Dispatcher[zio.Task] =
    zioRuntime
      .unsafeRun(
        cats.effect.std
          .Dispatcher[zio.Task]
          .allocated
      )
      ._1

  def mkTransactor(cfg: DBConfig): ZManaged[Any, Throwable, HikariTransactor[Task]] =
    for {
      rt <- ZIO.runtime[Any].toManaged
      xa <-
        HikariTransactor
          .newHikariTransactor[Task](
            cfg.driver,
            cfg.url,
            cfg.user,
            cfg.password,
            rt.runtimeConfig.executor.asExecutionContext
          )
          .toManaged
    } yield xa
}
