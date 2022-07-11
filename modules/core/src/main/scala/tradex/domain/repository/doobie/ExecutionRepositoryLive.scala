package tradex.domain
package repository.doobie

import zio._
import zio.prelude.NonEmptyList
import zio.interop.catz._

import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.execution._
import model.account._
import model.instrument._
import model.order._
import model.market._
import codecs._
import repository.ExecutionRepository
import tradex.domain.config._
import cats.effect.kernel.Resource
import java.time.ZonedDateTime

final case class ExecutionRepositoryLive(xaResource: Resource[Task, Transactor[Task]]) extends ExecutionRepository {
  import ExecutionRepositoryLive.SQL

  /** store */
  def store(exe: Execution): Task[Execution] =
    xaResource.use { xa =>
      SQL
        .insertExecution(exe)
        .run
        .transact(xa)
        .map(_ => exe)
        .orDie
    }

  /** store many executions */
  def storeMany(executions: NonEmptyList[Execution]): Task[Unit] =
    xaResource.use { xa =>
      SQL
        .insertMany(executions.toList)
        .transact(xa)
        .map(_ => ())
        .orDie
    }

  def deleteAll: Task[Unit] =
    xaResource.use { xa =>
      SQL.deleteAll.run
        .transact(xa)
        .map(_ => ())
        .orDie
    }
}

object ExecutionRepositoryLive extends CatzInterop {
  val layer: ZLayer[DBConfig, Throwable, ExecutionRepository] = {
    ZLayer
      .scoped(for {
        cfg        <- ZIO.service[DBConfig]
        transactor <- mkTransactor(cfg)
      } yield new ExecutionRepositoryLive(transactor))
  }

  object SQL {

    // when writing we have a valid `Execution` - hence we can use
    // Scala data types
    implicit val executionWrite: Write[Execution] =
      Write[
        (
            AccountNo,
            OrderNo,
            ISINCode,
            Market,
            BuySell,
            UnitPrice,
            Quantity,
            ZonedDateTime,
            Option[String]
        )
      ].contramap(execution =>
        (
          execution.accountNo,
          execution.orderNo,
          execution.isin,
          execution.market,
          execution.buySell,
          execution.unitPrice,
          execution.quantity,
          execution.dateOfExecution,
          execution.exchangeExecutionRefNo
        )
      )

    def insertExecution(exe: Execution): Update0 =
      sql"""
        INSERT INTO executions 
        (
          accountNo,
          orderNo,
          isinCode,
          market,
          buySellFlag,
          unitPrice,
          quantity,
          dateOfExecution,
          exchangeExecutionRefNo
        )
        VALUES 
  			(
  				${exe.accountNo},
  				${exe.orderNo},
  				${exe.isin},
  				${exe.market},
  				${exe.buySell}::buySell,
  				${exe.unitPrice},
  				${exe.quantity},
  				${exe.dateOfExecution},
  				${exe.exchangeExecutionRefNo}
  			)""".update

    def insertMany(executions: List[Execution]): ConnectionIO[Int] = {
      val sql = """
        INSERT INTO executions 
          (
            accountNo,
            orderNo,
            isinCode,
            market,
            buySellFlag,
            unitPrice,
            quantity,
            dateOfExecution,
            exchangeExecutionRefNo
          )
        VALUES 
			    (?, ?, ?, ?, ?::buySell, ?, ?, ?, ?)
       """
      Update[Execution](sql).updateMany(executions)
    }

    def deleteAll: Update0 = sql"""
      delete from executions
      """.update
  }
}
