package tradex.domain
package repository.inmemory

import zio._
import zio.prelude.NonEmptyList
import model.execution._
import repository.ExecutionRepository

final case class ExecutionRepositoryInMemory(state: Ref[Map[ExecutionReferenceNo, Execution]])
    extends ExecutionRepository {
  def store(exe: Execution): Task[Execution] =
    state
      .updateAndGet { m =>
        exe.executionRefNo
          .map { refNo =>
            m + ((refNo, exe))
          }
          .getOrElse {
            val ref = ExecutionReferenceNo(java.util.UUID.randomUUID())
            m + ((ref, exe.copy(executionRefNo = Some(ref))))
          }
      }
      .map(_ => exe)

  def storeMany(executions: NonEmptyList[Execution]): Task[Unit] = executions.forEach(t => store(t)).map(_ => ())
}

object ExecutionRepositoryInMemory {
  val layer: ULayer[ExecutionRepository] =
    Ref.make(Map.empty[ExecutionReferenceNo, Execution]).map(r => ExecutionRepositoryInMemory(r)).toLayer
}
