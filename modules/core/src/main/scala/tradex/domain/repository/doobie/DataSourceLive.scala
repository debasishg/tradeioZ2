package tradex.domain
package repository.doobie

import zio._
import zio.interop.catz._
import doobie.hikari._

import tradex.domain.config._
import cats.effect.kernel.Resource
import scala.concurrent.ExecutionContext

trait DataSourceService {
  def makeTransactor(ec: ExecutionContext): Task[Resource[Task, HikariTransactor[Task]]]
}

class DataSourceServiceLive(cfg: DBConfig) extends DataSourceService {
  import zio.interop.catz.implicits._

  implicit val zioRuntime = zio.Runtime.default.unsafe

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

  def makeTransactor(ec: ExecutionContext): Task[Resource[Task, HikariTransactor[Task]]] =
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

object DataSourceServiceLive {
  val layer: ZLayer[DBConfig, Throwable, DataSourceService] = {
    ZLayer
      .scoped(for {
        cfg <- ZIO.service[DBConfig]
      } yield new DataSourceServiceLive(cfg))
  }
}
