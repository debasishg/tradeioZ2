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