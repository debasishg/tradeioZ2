package tradex.domain

import zio.{ test => ztest, _ }
import zio.prelude._
import zio.test._
import zio.test.Assertion._

import generators._
import repository._
import model.account._
import repository.inmemory._
import services.trading._
import java.time._

object TradingServiceSpec {
  val localDateZERO = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)

  val spec = suite("Trading Service")(
    test("successfully invoke getAccountsOpenedOn") {
      check(Gen.listOfN(5)(accountGen)) { accounts =>
        for {
          _   <- TestClock.adjust(1.day)
          now <- zio.Clock.instant
          dt = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
          repo <- ZIO.service[AccountRepository]
          _ <- repo.store(
            accounts.map(_.copy(dateOfOpen = dt))
          )
          trading <- ZIO.service[TradingService]
          fetched <- trading.getAccountsOpenedOn(dt.toLocalDate())
        } yield assertTrue(
          fetched.forall(_.dateOfOpen.toLocalDate() == dt.toLocalDate())
        )
      }
    },
    test("successfully invoke getTrades") {
      check(accountGen) { account =>
        for {
          repo   <- ZIO.service[AccountRepository]
          stored <- repo.store(account)
        } yield assert(stored.accountType)(
          isOneOf(AccountType.values)
        )
      } *>
        check(Gen.listOfN(5)(tradeGen)) { trades =>
          for {
            repo <- ZIO.service[AccountRepository]
            accs <- repo.all
            _    <- TestClock.adjust(1.day)
            now  <- zio.Clock.instant
            dt                    = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
            tradesTodayForAccount = trades.map(_.copy(accountNo = accs.head.no, tradeDate = dt))
            _ <- TradeRepository.storeNTrades(
              NonEmptyList(tradesTodayForAccount.head, tradesTodayForAccount.tail: _*)
            )
            trading <- ZIO.service[TradingService]
            fetched <- trading.getTrades(accs.head.no, Some(dt.toLocalDate()))
          } yield assertTrue(
            fetched.forall(_.tradeDate.toLocalDate() == dt.toLocalDate())
          )
        }
    },
    test("successfully invoke orders") {
      check(Gen.listOfN(5)(frontOfficeOrderGen)) { foOrders =>
        for {
          trading <- ZIO.service[TradingService]
          os      <- trading.orders(NonEmptyList(foOrders.head, foOrders.tail: _*))
        } yield assertTrue(
          os.size > 0
        )
      }
    }
  ).provide(
    TradingServiceTest.layer,
    TestRandom.deterministic,
    Sized.default
  )

}

object TradingServiceTest {
  val serviceLayer: URLayer[
    AccountRepository with OrderRepository with ExecutionRepository with TradeRepository,
    TradingService
  ] = {
    ZLayer.fromFunction(TradingServiceLive(_, _, _, _))
  }
  val layer =
    (AccountRepositoryInMemory.layer ++
      OrderRepositoryInMemory.layer ++
      ExecutionRepositoryInMemory.layer ++
      TradeRepositoryInMemory.layer) >+> serviceLayer
}
