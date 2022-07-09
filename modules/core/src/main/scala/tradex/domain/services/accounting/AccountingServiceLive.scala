package tradex.domain
package services.accounting

import zio._
import zio.prelude._
import model.account._
import model.trade._
import model.balance._
import java.time.LocalDate
import repository.BalanceRepository

case class AccountingError(cause: String) extends Throwable
final case class AccountingServiceLive(br: BalanceRepository) extends AccountingService {

  def postBalance(trade: Trade): IO[AccountingError, Balance] = {
    withAccountingService(
      trade.netAmount
        .map { amt =>
          br.store(
            Balance(trade.accountNo, amt, amt.currency, today)
          )
        }
        .getOrElse(ZIO.fail(AccountingError(s"No net amount to post for $trade")))
    )
  }

  def postBalance(trades: NonEmptyList[Trade]): IO[AccountingError, NonEmptyList[Balance]] =
    trades.forEach(postBalance)

  def getBalance(accountNo: AccountNo): IO[AccountingError, Option[Balance]] =
    withAccountingService(br.queryBalanceByAccountNo(accountNo))

  def getBalanceByDate(date: LocalDate): IO[AccountingError, List[Balance]] =
    withAccountingService(br.queryBalanceAsOf(date))

  private def withAccountingService[A](t: Task[A]): IO[AccountingError, A] =
    t.mapError(th => AccountingError(th.getMessage))
}

object AccountingServiceLive {
  val layer = ZLayer.fromFunction(AccountingServiceLive.apply _)
}
