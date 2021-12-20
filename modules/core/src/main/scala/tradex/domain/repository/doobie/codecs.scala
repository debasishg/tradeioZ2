package tradex.domain
package repository.doobie

import java.util.UUID
import squants.market._
import doobie._
import doobie.postgres.implicits._

import NewtypeRefinedOps._
import model.account._
import model.instrument._
import model.order._
import model.user._
import model.execution._
import model.market._
import model.trade._

object codecs {
  private def accountNoFromString(s: String) = {
    validate[AccountNo](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val accountNoMeta: Meta[AccountNo] =
    Meta[String].timap(accountNoFromString)(_.value.value)

  private def accountNameFromString(s: String) = {
    validate[AccountName](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val accountNameMeta: Meta[AccountName] =
    Meta[String].timap(accountNameFromString)(_.value.value)

  implicit val accountTypeMeta: Meta[AccountType] =
    Meta[String].timap(s => AccountType.withName(s))(_.entryName)

  implicit val currencyMeta: Meta[Currency] = Meta[String].timap(s => Currency(s).get)(_.name)

  private def isinFromString(s: String) = {
    validate[ISINCode](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val isinCodeMeta: Meta[ISINCode] =
    Meta[String].timap(isinFromString)(_.value.value)

  private def instrumentNameFromString(s: String) = {
    validate[InstrumentName](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val instrumentNameMeta: Meta[InstrumentName] =
    Meta[String].timap(instrumentNameFromString)(_.value.value)

  implicit val instrumentTypeMeta: Meta[InstrumentType] =
    Meta[String].timap(s => InstrumentType.withName(s))(_.entryName)

  private def lotSizeFromInt(s: Int) = {
    validate[LotSize](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val lotSizeMeta: Meta[LotSize] =
    Meta[Int].timap(lotSizeFromInt)(_.value.value)

  private def unitPriceFromBigDecimal(s: BigDecimal) = {
    validate[UnitPrice](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val unitPriceMeta: Meta[UnitPrice] =
    Meta[BigDecimal].timap(unitPriceFromBigDecimal)(_.value.value)

  implicit val moneyMeta: Meta[Money] =
    Meta[BigDecimal].timap(s => USD(s))(_.amount)

  private def orderNoFromString(s: String) = {
    validate[OrderNo](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val orderNoMeta: Meta[OrderNo] =
    Meta[String].timap(orderNoFromString)(_.value.value)

  private def quantityFromBigDecimal(s: BigDecimal) = {
    validate[Quantity](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val quantityMeta: Meta[Quantity] =
    Meta[BigDecimal].timap(quantityFromBigDecimal)(_.value.value)

  implicit val buySellMeta: Meta[BuySell] =
    Meta[String].timap(s => BuySell.withName(s))(_.entryName)

  implicit val userIdMeta: Meta[UserId] =
    Meta[UUID].timap(u => UserId(u))(_.value)

  private def userNameFromString(s: String) = {
    validate[UserName](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val userNameMeta: Meta[UserName] =
    Meta[String].timap(userNameFromString)(_.value.value)

  private def passwordFromString(s: String) = {
    validate[EncryptedPassword](s).fold(errs => throw new Exception(errs.toString), identity)
  }
  implicit val passwordMeta: Meta[EncryptedPassword] =
    Meta[String].timap(passwordFromString)(_.value.value)

  implicit val executionReferenceNoMeta: Meta[ExecutionReferenceNo] =
    Meta[UUID].timap(u => ExecutionReferenceNo(u))(_.value)

  implicit val marketMeta: Meta[Market] =
    Meta[String].timap(s => Market.withName(s))(_.entryName)

  implicit val tradeReferenceNoMeta: Meta[TradeReferenceNo] =
    Meta[UUID].timap(u => TradeReferenceNo(u))(_.value)

  implicit val taxFeeIdMeta: Meta[TaxFeeId] =
    Meta[String].timap(s => TaxFeeId.withName(s))(_.entryName)
}
