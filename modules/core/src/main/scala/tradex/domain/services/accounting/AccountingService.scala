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
