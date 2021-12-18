package tradex.domain
package model

import java.time.LocalDateTime
import java.time.Instant
import zio.prelude._

import instrument._
import account._
import NewtypeRefinedOps._
import enumeratum._
import io.estatico.newtype.macros.newtype

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.auto._
import derevo.circe.magnolia._
import derevo.derive
import io.circe.refined._

object order {
  @derive(decoder, encoder)
  @newtype case class OrderNo(value: NonEmptyString)

  type QuantityType = BigDecimal Refined NonNegative
  @derive(decoder, encoder)
  @newtype case class Quantity(value: BigDecimal Refined NonNegative)

  type UnitPriceType = BigDecimal Refined Positive
  @derive(decoder, encoder)
  @newtype case class UnitPrice(value: UnitPriceType)

  @derive(decoder, encoder)
  sealed abstract class BuySell(override val entryName: String) extends EnumEntry

  @derive(decoder, encoder)
  object BuySell extends Enum[BuySell] {
    case object Buy  extends BuySell("buy")
    case object Sell extends BuySell("sell")

    val values = findValues
  }

  // domain entity
  @derive(decoder, encoder)
  private[domain] final case class LineItem private (
      orderNo: OrderNo,
      instrument: ISINCode,
      quantity: Quantity,
      unitPrice: UnitPrice,
      buySell: BuySell
  )

  @derive(decoder, encoder)
  final case class Order private (
      no: OrderNo,
      date: LocalDateTime,
      accountNo: AccountNo,
      items: NonEmptyList[LineItem]
  )

  @derive(decoder, encoder)
  final case class FrontOfficeOrder private (
      accountNo: String,
      date: Instant,
      isin: String,
      qty: BigDecimal,
      unitPrice: BigDecimal,
      buySell: String
  )

  object Order {
    private def makeOrderNo(accountNo: String, date: Instant): String = s"$accountNo-${date.hashCode}"

    /** Domain validation for `FrontOfficeOrder` is done here. Creates records after validation
      */
    private[domain] def create(
        frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
    ): Validation[String, List[Order]] = {
      Validation.validateAll(
        frontOfficeOrders.toList
          .groupBy(_.accountNo)
          .map { case (ano, forders) =>
            makeOrder(makeOrderNo(ano, Instant.now), today, ano, forders)
          }
          .toList
      )
    }

    private[domain] def makeOrder(
        ono: String,
        odt: LocalDateTime,
        ano: String,
        forders: List[FrontOfficeOrder]
    ): Validation[String, Order] = {
      val lineItems = Validation.validateAll(forders.map { fo =>
        makeLineItem(ono, fo.isin, fo.qty, fo.unitPrice, fo.buySell)
      })

      lineItems match {
        case Validation.Success(_, items) =>
          makeOrder(ono, odt, ano, NonEmptyList(items.head, items.tail: _*))
        case Validation.Failure(_, es) => Validation.fail(es.toList.mkString("/"))
      }
    }

    private[domain] def makeLineItem(
        ono: String,
        isin: String,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        buySell: String
    ): Validation[String, LineItem] = {
      Validation.validateWith(
        validateOrderNo(ono),
        Instrument.validateISINCode(isin),
        validateQuantity(quantity),
        validateUnitPrice(unitPrice),
        validateBuySell(buySell)
      ) { (orderNo, isin, qty, price, bs) =>
        LineItem(orderNo, isin, qty, price, BuySell.withName(bs))
      }
    }

    private[domain] def makeOrder(
        ono: String,
        orderDate: LocalDateTime,
        accountNo: String,
        lineItems: NonEmptyList[LineItem]
    ): Validation[String, Order] = {
      Validation.validateWith(
        validateOrderNo(ono),
        Account.validateAccountNo(accountNo)
      ) { (orderNo, accountNo) =>
        Order(
          orderNo,
          orderDate,
          accountNo,
          lineItems
        )
      }
    }

    private[model] def validateQuantity(
        qty: BigDecimal
    ): Validation[String, Quantity] = {
      validate[Quantity](qty)
        .mapError(s => s"Quantity has to be positive: found $qty (root cause: $s)")
    }

    private[model] def validateUnitPrice(
        price: BigDecimal
    ): Validation[String, UnitPrice] = {
      validate[UnitPrice](price)
        .mapError(s => s"Unit Price has to be positive: found $price (root cause: $s)")
    }

    private[model] def validateOrderNo(
        orderNo: String
    ): Validation[String, OrderNo] = {
      validate[OrderNo](orderNo)
    }

    private[model] def validateBuySell(
        bs: String
    ): Validation[String, String] = {
      Validation
        .fromEither(BuySell.withNameEither(bs).map(_.entryName))
        .mapError(_ => s"Invalid buySell $bs")
    }
  }
}
