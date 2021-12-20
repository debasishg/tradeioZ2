package tradex.domain
package repository

import zio._
import zio.prelude.NonEmptyList
import model.execution._

trait ExecutionRepository {

  /** store */
  def store(exe: Execution): Task[Execution]

  /** store many executions */
  def storeMany(executions: NonEmptyList[Execution]): Task[Unit]
}

object ExecutionRepository {
  def store(exe: Execution): RIO[ExecutionRepository, Execution] =
    ZIO.serviceWithZIO(_.store(exe))

  def storeMany(executions: NonEmptyList[Execution]): RIO[ExecutionRepository, Unit] =
    ZIO.serviceWithZIO(_.storeMany(executions))
}
