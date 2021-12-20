package tradex.domain
package repository

import zio._
import java.time.LocalDate
import model.account.AccountNo
import model.balance._

trait BalanceRepository {

  /** query by account number */
  def queryBalanceByAccountNo(no: AccountNo): Task[Option[Balance]]

  /** store */
  def store(a: Balance): Task[Balance]

  /** query all balances that have amount as of this date */
  /** asOf date <= this date */
  def queryBalanceAsOf(date: LocalDate): Task[List[Balance]]

  /** all balances */
  def allBalances: Task[List[Balance]]
}

object BalanceRepository {
  def queryBalanceByAccountNo(no: AccountNo): RIO[BalanceRepository, Option[Balance]] =
    ZIO.serviceWithZIO(_.queryBalanceByAccountNo(no))

  def store(b: Balance): RIO[BalanceRepository, Balance] =
    ZIO.serviceWithZIO(_.store(b))

  def queryBalanceAsOf(date: LocalDate): RIO[BalanceRepository, List[Balance]] =
    ZIO.serviceWithZIO(_.queryBalanceAsOf(date))

  def allBalances: RIO[BalanceRepository, List[Balance]] =
    ZIO.serviceWithZIO(_.allBalances)
}
