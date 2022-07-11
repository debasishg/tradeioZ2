package tradex.domain
package model

import zio.prelude._
import derevo.circe.magnolia._
import derevo.derive
import squants.market._
import account._
import java.time.ZonedDateTime

object balance {
  @derive(decoder, encoder)
  final case class Balance private (
      accountNo: AccountNo,
      amount: Money,
      currency: Currency,
      asOf: ZonedDateTime
  )

  object Balance {
    def balance(
        accountNo: String,
        amount: Money,
        currency: Currency,
        asOf: ZonedDateTime
    ): Validation[String, Balance] = {
      Validation.validateWith(
        Account.validateAccountNo(accountNo),
        validateAsOfDate(asOf)
      ) { (ano, dt) =>
        Balance(ano, amount, currency, dt)
      }
    }

    private def validateAsOfDate(
        date: ZonedDateTime
    ): Validation[String, ZonedDateTime] =
      if (date.isAfter(today))
        Validation.fail(s"Balance date [$date] cannot be later than today")
      else Validation.succeed(date)
  }
}
