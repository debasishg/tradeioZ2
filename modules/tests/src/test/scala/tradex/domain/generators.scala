package tradex.domain

import java.time._
import eu.timepit.refined.api.RefType
import eu.timepit.refined.auto._

import zio.prelude._
import zio.Random
import zio.test._
import zio.test.Gen.fromZIOSample

import squants.market.USD
import squants.market.JPY

import model.account._
import model.instrument._
import model.order._
import model.user._
import model.market._
import model.trade._
import NewtypeRefinedOps._

object generators {
  val posIntGen =
    fromZIOSample(Random.nextIntBetween(0, Int.MaxValue).map(Sample.shrinkIntegral(0)))

  val nonEmptyStringGen: Gen[Random with Sized, String]  = Gen.alphaNumericStringBounded(21, 40)
  val accountNoStringGen: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(5, 12)
  val accountNoGen: Gen[Random with Sized, AccountNo] =
    accountNoStringGen.map(str =>
      validate[AccountNo](str)
        .fold(errs => throw new Exception(errs.toString), identity)
    )

  val accountNameGen: Gen[Random with Sized, AccountName] =
    nonEmptyStringGen
      .map(s => validate[AccountName](s))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  def accountNameStartingPatternGen(pattern: String): Gen[Random with Sized, AccountName] =
    nonEmptyStringGen
      .map(s => validate[AccountName](s"$pattern$s"))
      .map(_.fold(errs => throw new Exception(errs.toString), identity))

  def openCloseDateGen = for {
    o <- Gen.fromIterable(List(LocalDateTime.now, LocalDateTime.now.plusDays(2)))
    c <- Gen.const(o.plusDays(100))
  } yield (o, c)

  def accountWithNamePatternGen(pattern: String) = for {
    no <- accountNoGen
    nm <- accountNameStartingPatternGen(pattern)
    oc <- openCloseDateGen
    tp <- Gen.fromIterable(AccountType.values)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.const(None)
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val tradingAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Trading)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.const(None)
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val settlementAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Settlement)
    bc <- Gen.const(USD)
    tc <- Gen.const(None)
    sc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  val bothAccountGen: Gen[Random with Sized, Account] = for {
    no <- accountNoGen
    nm <- accountNameGen
    oc <- openCloseDateGen
    tp <- Gen.const(AccountType.Both)
    bc <- Gen.const(USD)
    tc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
    sc <- Gen.fromIterable(List(USD, JPY)).map(Some(_))
  } yield Account(no, nm, oc._1, Some(oc._2), tp, bc, tc, sc)

  def accountGen: Gen[Random with Sized, Account] =
    Gen.oneOf(tradingAccountGen, settlementAccountGen, bothAccountGen)

  def isinGen: Gen[Any, ISINCode] = {
    val appleISINStr = "US0378331005"
    val baeISINStr   = "GB0002634946"
    val ibmISINStr   = "US4592001014"

    val isins = List(appleISINStr, baeISINStr, ibmISINStr)
      .map(str =>
        RefType
          .applyRef[ISINCodeString](str)
          .map(ISINCode(_))
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(isins)
  }

  val unitPriceGen: Gen[Any, UnitPrice] = {
    val ups = List(BigDecimal(12.25), BigDecimal(51.25), BigDecimal(55.25))
      .map(n =>
        RefType
          .applyRef[UnitPriceType](n)
          .map(UnitPrice(_))
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(ups)
  }

  val quantityGen: Gen[Any, Quantity] = {
    val qtys = List(BigDecimal(100), BigDecimal(200), BigDecimal(300))
      .map(n =>
        RefType
          .applyRef[QuantityType](n)
          .map(Quantity(_))
          .fold(err => throw new Exception(err), identity)
      )
    Gen.fromIterable(qtys)
  }

  val userIdGen: Gen[Random, UserId] = Gen.uuid.map(UserId(_))

  val tradeGen: Gen[Random with Sized, Trade] = for {
    no   <- accountNoGen
    isin <- isinGen
    mkt  <- Gen.fromIterable(Market.values)
    bs   <- Gen.fromIterable(BuySell.values)
    up   <- unitPriceGen
    qty  <- quantityGen
    td   <- Gen.fromIterable(List(LocalDateTime.now, LocalDateTime.now.plusDays(2)))
    vd   <- Gen.const(None)
    uid  <- userIdGen
  } yield Trade
    .trade(no, isin, mkt, bs, up, qty, td, vd, Some(uid))
    .fold(errs => throw new Exception(errs.toString), identity)

  val frontOfficeOrderGen = for {
    ano  <- accountNoGen
    dt   <- Gen.fromIterable(List(Instant.now, Instant.now.plus(2, java.time.temporal.ChronoUnit.DAYS)))
    isin <- isinGen
    qty  <- quantityGen
    up   <- unitPriceGen
    bs   <- Gen.fromIterable(BuySell.values)
  } yield FrontOfficeOrder(ano.value.value, dt, isin.value.value, qty.value.value, up.value.value, bs.entryName)

  def frontOfficeOrderGenWithAccountAndInstrument(accs: List[AccountNo], ins: List[ISINCode]) = for {
    ano  <- Gen.fromIterable(accs)
    dt   <- Gen.fromIterable(List(Instant.now, Instant.now.plus(2, java.time.temporal.ChronoUnit.DAYS)))
    isin <- Gen.fromIterable(ins)
    qty  <- quantityGen
    up   <- unitPriceGen
    bs   <- Gen.fromIterable(BuySell.values)
  } yield FrontOfficeOrder(ano.value.value, dt, isin.value.value, qty.value.value, up.value.value, bs.entryName)

  def generateTradeFrontOfficeInputGenWithAccountAndInstrument(accs: List[AccountNo], ins: List[ISINCode]) = for {
    orders       <- Gen.listOfN(3)(frontOfficeOrderGenWithAccountAndInstrument(accs, ins))
    mkt          <- Gen.fromIterable(Market.values)
    brkAccountNo <- Gen.fromIterable(accs)
  } yield GenerateTradeFrontOfficeInput(
    NonEmptyList(orders.head, orders.tail: _*),
    mkt,
    brkAccountNo,
    NonEmptyList(accs.head, accs.tail: _*)
  )

  def tradeGnerationInputGen = for {
    a <- accountGen
    i <- isinGen
    u <- userIdGen
  } yield (a, i, u)
}
