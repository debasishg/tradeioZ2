package tradex.domain
package repository.inmemory

import zio._
import java.time._
import model.account._
import model.balance._
import repository.BalanceRepository

final case class BalanceRepositoryInMemory(state: Ref[Map[AccountNo, Balance]]) extends BalanceRepository {

  def queryBalanceByAccountNo(no: AccountNo): Task[Option[Balance]] = state.get.map(_.get(no))

  def store(b: Balance): Task[Balance] = state.update(m => m + ((b.accountNo, b))).map(_ => b)

  def queryBalanceAsOf(date: LocalDate): Task[List[Balance]] =
    state.get.map(_.values.filter(_.asOf.toLocalDate == date).toList)

  def allBalances: Task[List[Balance]] = state.get.map(_.values.toList)
}

object BalanceRepositoryInMemory {
  val layer: ULayer[BalanceRepository] =
    ZLayer.scoped(
      Ref.make(Map.empty[AccountNo, Balance]).map(BalanceRepositoryInMemory(_))
    )
}
