package tradex.domain
package services.accounting

import zio._
import zio.prelude._
import model.account._
import model.trade._
import model.balance._
import java.time.LocalDate

trait AccountingService {
  def postBalance(trade: Trade): IO[AccountingError, Balance]
  def postBalance(trades: NonEmptyList[Trade]): IO[AccountingError, NonEmptyList[Balance]]
  def getBalance(accountNo: AccountNo): IO[AccountingError, Option[Balance]]
  def getBalanceByDate(date: LocalDate): IO[AccountingError, List[Balance]]
}

object AccountingService {
  def postBalance(trade: Trade): ZIO[AccountingService, AccountingError, Balance] =
    ZIO.serviceWithZIO(_.postBalance(trade))

  def postBalance(trades: NonEmptyList[Trade]): ZIO[AccountingService, AccountingError, NonEmptyList[Balance]] =
    ZIO.serviceWithZIO(_.postBalance(trades))

  def getBalance(accountNo: AccountNo): ZIO[AccountingService, AccountingError, Option[Balance]] =
    ZIO.serviceWithZIO(_.getBalance(accountNo))

  def getBalanceByDate(date: LocalDate): ZIO[AccountingService, AccountingError, List[Balance]] =
    ZIO.serviceWithZIO(_.getBalanceByDate(date))
}
