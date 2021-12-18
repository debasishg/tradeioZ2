package tradex.domain
package model

import java.time.LocalDateTime
import java.util.UUID

import zio.prelude._

import enumeratum._

import squants.market._

import account._
import instrument._
import order._
import market._
import user.UserId
import io.estatico.newtype.macros.newtype
import derevo.circe.magnolia._
import derevo.derive

object trade {
  @derive(decoder, encoder)
  @newtype case class TradeReferenceNo(value: UUID)

  @derive(decoder, encoder)
  sealed abstract class TaxFeeId(override val entryName: String) extends EnumEntry

  @derive(decoder, encoder)
  object TaxFeeId extends Enum[TaxFeeId] {
    case object TradeTax   extends TaxFeeId("TradeTax")
    case object Commission extends TaxFeeId("Commission")
    case object VAT        extends TaxFeeId("VAT")
    case object Surcharge  extends TaxFeeId("Surcharge")

    val values = findValues
  }

  @derive(decoder, encoder)
  final case class GenerateTradeFrontOfficeInput(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder],
      market: Market,
      brokerAccountNo: AccountNo,
      clientAccountNos: NonEmptyList[AccountNo]
  )

  import TaxFeeId._

  // rates of tax/fees expressed as fractions of the principal of the trade
  final val rates: Map[TaxFeeId, BigDecimal] =
    Map(TradeTax -> 0.2, Commission -> 0.15, VAT -> 0.1)

  // tax and fees applicable for each market
  // Other signifies the general rule applicable for all markets
  final val taxFeeForMarket: Map[Market, List[TaxFeeId]] =
    Map(
      Market.Other     -> List(TradeTax, Commission),
      Market.Singapore -> List(TradeTax, Commission, VAT)
    )

  // get the list of tax/fees applicable for this trade
  // depending on the market
  final val forTrade: Trade => Option[List[TaxFeeId]] = { trade =>
    taxFeeForMarket.get(trade.market).orElse(taxFeeForMarket.get(Market.Other))
  }

  final def principal(trade: Trade): Money =
    Money(trade.unitPrice.value.value * trade.quantity.value.value)

  // combinator to value a tax/fee for a specific trade
  private def valueAs(trade: Trade, taxFeeId: TaxFeeId): Money = {
    ((rates get taxFeeId) map (_ * principal(trade))) getOrElse (Money(0))
  }

  // all tax/fees for a specific trade
  private def taxFeeCalculate(
      trade: Trade,
      taxFeeIds: List[TaxFeeId]
  ): List[TradeTaxFee] = {
    taxFeeIds
      .zip(taxFeeIds.map(valueAs(trade, _)))
      .map { case (tid, amt) => TradeTaxFee(tid, amt) }
  }

  private def netAmount(
      trade: Trade,
      taxFeeAmounts: List[TradeTaxFee]
  ): Money = {
    principal(trade) + taxFeeAmounts.map(_.amount).foldLeft(Money(0))(_ + _)
  }

  @derive(decoder, encoder)
  final case class Trade private (
      accountNo: AccountNo,
      isin: ISINCode,
      market: Market,
      buySell: BuySell,
      unitPrice: UnitPrice,
      quantity: Quantity,
      tradeDate: LocalDateTime = today,
      valueDate: Option[LocalDateTime] = None,
      userId: Option[UserId] = None,
      taxFees: List[TradeTaxFee] = List.empty,
      netAmount: Option[Money] = None,
      tradeRefNo: Option[TradeReferenceNo] = None
  )

  @derive(decoder, encoder)
  private[domain] final case class TradeTaxFee(
      taxFeeId: TaxFeeId,
      amount: Money
  )

  object Trade {
    def trade(
        accountNo: AccountNo,
        isin: ISINCode,
        market: Market,
        buySell: BuySell,
        unitPrice: UnitPrice,
        quantity: Quantity,
        tradeDate: LocalDateTime = today,
        valueDate: Option[LocalDateTime] = None,
        userId: Option[UserId] = None
    ): Validation[String, Trade] = {
      validateTradeValueDate(tradeDate, valueDate).map { case (td, maybeVd) =>
        Trade(
          accountNo,
          isin,
          market,
          buySell,
          unitPrice,
          quantity,
          td,
          maybeVd,
          userId
        )
      }
    }

    private def validateTradeValueDate(
        td: LocalDateTime,
        vd: Option[LocalDateTime]
    ): Validation[String, (LocalDateTime, Option[LocalDateTime])] = {
      vd.map { v =>
        if (v.isBefore(td)) Validation.fail(s"Value date $v cannot be earlier than trade date $td")
        else Validation.succeed((td, vd))
      }.getOrElse(Validation.succeed((td, vd)))
    }

    def withTaxFee(trade: Trade): Trade = {
      if (trade.taxFees.isEmpty && !trade.netAmount.isDefined) {
        val taxFees =
          forTrade(trade).map(taxFeeCalculate(trade, _)).getOrElse(List.empty)
        val netAmt = netAmount(trade, taxFees)
        trade.copy(taxFees = taxFees, netAmount = Option(netAmt))
      } else trade
    }
  }
}
