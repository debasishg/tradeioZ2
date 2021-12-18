package tradex.domain
package model

import java.time.LocalDateTime
import zio.prelude._

import squants.market._

import NewtypeRefinedOps._
import enumeratum._
import io.estatico.newtype.macros.newtype

import eu.timepit.refined.numeric._
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import eu.timepit.refined.boolean.AllOf
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.auto._
import derevo.circe.magnolia._
import derevo.derive
import io.circe.refined._

import model.order.UnitPrice

import _root_.shapeless.::
import _root_.shapeless.HNil

object instrument {
  type ISINCodeString = String Refined AllOf[
    MaxSize[W.`12`.T] ::
      MinSize[W.`12`.T] ::
      MatchesRegex[W.`"([A-Z]{2})((?![A-Z]{10}\b)[A-Z0-9]{10})"`.T] ::
      HNil
  ]

  @derive(decoder, encoder)
  @newtype case class ISINCode(value: ISINCodeString)

  @derive(decoder, encoder)
  @newtype case class InstrumentName(value: NonEmptyString)

  type LotSizeType = Int Refined Positive
  @derive(decoder, encoder)
  @newtype case class LotSize(value: LotSizeType)

  @derive(decoder, encoder)
  sealed abstract class InstrumentType(override val entryName: String) extends EnumEntry

  @derive(decoder, encoder)
  object InstrumentType extends Enum[InstrumentType] {
    case object CCY         extends InstrumentType("ccy")
    case object Equity      extends InstrumentType("equity")
    case object FixedIncome extends InstrumentType("fixedincome")

    val values = findValues
  }

  @derive(decoder, encoder)
  final case class Instrument private (
      isinCode: ISINCode,
      name: InstrumentName,
      instrumentType: InstrumentType,
      dateOfIssue: Option[LocalDateTime],    // for non CCY
      dateOfMaturity: Option[LocalDateTime], // for Fixed Income
      lotSize: LotSize,
      unitPrice: Option[UnitPrice],       // for Equity
      couponRate: Option[Money],          // for Fixed Income
      couponFrequency: Option[BigDecimal] // for Fixed Income
  )

  @derive(decoder, encoder)
  final case class CreateInstrument private (
      isinCode: String,
      name: String,
      instrumentType: InstrumentType,
      dateOfIssue: Option[LocalDateTime],    // for non CCY
      dateOfMaturity: Option[LocalDateTime], // for Fixed Income
      lotSize: Int,
      unitPrice: Option[BigDecimal],      // for Equity
      couponRate: Option[Money],          // for Fixed Income
      couponFrequency: Option[BigDecimal] // for Fixed Income
  ) {
    def toDomain: Validation[String, Instrument] = {
      Instrument.instrument(
        isinCode,
        name,
        instrumentType,
        dateOfIssue,
        dateOfMaturity,
        Some(lotSize),
        unitPrice,
        couponRate,
        couponFrequency
      )
    }
  }

  object Instrument {
    private[model] def validateISINCode(
        isin: String
    ): Validation[String, ISINCode] = validate[ISINCode](isin)

    private[model] def validateInstrumentName(
        name: String
    ): Validation[String, InstrumentName] =
      validate[InstrumentName](name)

    private[model] def validateLotSize(
        size: Int
    ): Validation[String, LotSize] = validate[LotSize](size)

    private[model] def validateUnitPrice(
        price: BigDecimal
    ): Validation[String, UnitPrice] = {
      validate[UnitPrice](price)
        .mapError(s => s"Unit Price has to be positive: found $price (root cause: $s")
    }

    private[domain] def instrument(
        isinCode: String,
        name: String,
        instrumentType: InstrumentType,
        dateOfIssue: Option[LocalDateTime],    // for non CCY
        dateOfMaturity: Option[LocalDateTime], // for Fixed Income
        lotSize: Option[Int],
        unitPrice: Option[BigDecimal],      // for Equity
        couponRate: Option[Money],          // for Fixed Income
        couponFrequency: Option[BigDecimal] // for Fixed Income
    ): Validation[String, Instrument] = instrumentType match {
      case InstrumentType.CCY    => ccy(isinCode, name, dateOfIssue)
      case InstrumentType.Equity => equity(isinCode, name, dateOfIssue, lotSize, unitPrice)
      case InstrumentType.FixedIncome =>
        fixedIncome(isinCode, name, dateOfIssue, dateOfMaturity, lotSize, couponRate, couponFrequency)
    }

    private[domain] def ccy(
        isinCode: String,
        name: String,
        dateOfIssue: Option[LocalDateTime] // for non CCY
    ): Validation[String, Instrument] = {
      Validation.validateWith(
        validateISINCode(isinCode),
        validateInstrumentName(name)
      ) { (isin, name) =>
        Instrument(
          isin,
          name,
          InstrumentType.CCY,
          dateOfIssue,
          None,
          LotSize(1),
          None,
          None,
          None
        )
      }
    }

    private[domain] def fixedIncome(
        isinCode: String,
        name: String,
        dateOfIssue: Option[LocalDateTime],    // for non CCY
        dateOfMaturity: Option[LocalDateTime], // for Fixed Income
        lotSize: Option[Int],
        couponRate: Option[Money],          // for Fixed Income
        couponFrequency: Option[BigDecimal] // for Fixed Income
    ): Validation[String, Instrument] = {
      Validation.validateWith(
        validateISINCode(isinCode),
        validateInstrumentName(name),
        validateLotSize(lotSize.getOrElse(0))
      ) { (isin, name, ls) =>
        Instrument(
          isin,
          name,
          InstrumentType.FixedIncome,
          dateOfIssue,
          dateOfMaturity,
          ls,
          None,
          couponRate,
          couponFrequency
        )
      }
    }

    private[domain] def equity(
        isinCode: String,
        name: String,
        dateOfIssue: Option[LocalDateTime], // for non CCY
        lotSize: Option[Int],
        unitPrice: Option[BigDecimal] // for Equity
    ): Validation[String, Instrument] = {
      Validation.validateWith(
        validateISINCode(isinCode),
        validateInstrumentName(name),
        validateLotSize(lotSize.getOrElse(0)),
        validateUnitPrice(unitPrice.getOrElse(ZERO_BIG_DECIMAL))
      ) { (isin, name, ls, uprice) =>
        Instrument(
          isin,
          name,
          InstrumentType.Equity,
          dateOfIssue,
          None,
          ls,
          Some(uprice),
          None,
          None
        )
      }
    }
  }
}
