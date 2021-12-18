package tradex.domain
package model

import java.time.LocalDateTime
import zio.prelude._
import derevo.circe.magnolia._
import derevo.derive
import squants.market._
import account._

object balance {
  @derive(decoder, encoder)
  final case class Balance private (
      accountNo: AccountNo,
      amount: Money,
      currency: Currency,
      asOf: LocalDateTime
  )

  object Balance {
    def balance(
        accountNo: String,
        amount: Money,
        currency: Currency,
        asOf: LocalDateTime
    ): Validation[String, Balance] = {
      Validation.validateWith(
        Account.validateAccountNo(accountNo),
        validateAsOfDate(asOf)
      ) { (ano, dt) =>
        Balance(ano, amount, currency, dt)
      }
    }

    private def validateAsOfDate(
        date: LocalDateTime
    ): Validation[String, LocalDateTime] =
      if (date.isAfter(today))
        Validation.fail(s"Balance date [$date] cannot be later than today")
      else Validation.succeed(date)
  }
}
