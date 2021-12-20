package tradex.domain

import zio._
import zio.prelude._
import zio.test._
import zio.test.Assertion._
import zio.test.TestClock

import generators._
import repository._
import model.account._
import repository.inmemory._
import services.trading._
import java.time._

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object TradingServiceSpec extends DefaultRunnableSpec {
  val localDateZERO = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
  val spec = suite("Trading Service")(
    test("successfully invoke getAccountsOpenedOn") {
      check(Gen.listOfN(5)(accountGen)) { accounts =>
        for {
          _   <- TestClock.adjust(1.day)
          now <- zio.Clock.instant
          dt = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
          _ <- AccountRepository.store(
            accounts.map(_.copy(dateOfOpen = dt))
          )
          fetched <- TradingService.getAccountsOpenedOn(dt.toLocalDate())
        } yield assertTrue(
          fetched.forall(_.dateOfOpen.toLocalDate() == dt.toLocalDate())
        )
      }
    }.provideCustomLayer(TradingServiceTest.layer),
    test("successfully invoke getTrades") {
      check(accountGen) { account =>
        for {
          stored <- AccountRepository.store(account)
        } yield assert(stored.accountType)(
          isOneOf(AccountType.values)
        )
      } *>
        check(Gen.listOfN(5)(tradeGen)) { trades =>
          for {
            accs <- AccountRepository.all
            _    <- TestClock.adjust(1.day)
            now  <- zio.Clock.instant
            dt                    = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
            tradesTodayForAccount = trades.map(_.copy(accountNo = accs.head.no, tradeDate = dt))
            _ <- TradeRepository.storeNTrades(
              NonEmptyList(tradesTodayForAccount.head, tradesTodayForAccount.tail: _*)
            )
            fetched <- TradingService.getTrades(accs.head.no, Some(dt.toLocalDate()))
          } yield assertTrue(
            fetched.forall(_.tradeDate.toLocalDate() == dt.toLocalDate())
          )
        }
    }.provideCustomLayer(TradingServiceTest.layer),
    test("successfully invoke orders") {
      check(Gen.listOfN(5)(frontOfficeOrderGen)) { foOrders =>
        for {
          os <- TradingService.orders(NonEmptyList(foOrders.head, foOrders.tail: _*))
        } yield assertTrue(
          os.size > 0
        )
      }
    }.provideCustomLayer(TradingServiceTest.layer),
    test("successfully generate trades from front office input") {
      check(tradeGnerationInputGen) { case (account, isin, userId) =>
        check(generateTradeFrontOfficeInputGenWithAccountAndInstrument(List(account.no), List(isin))) { foInput =>
          ZIO.succeed(println(foInput.asJson.printWith(Printer.noSpaces))) *>
            (for {
              trades <- TradingService.generateTrade(foInput, userId)
            } yield assertTrue(
              trades.size > 0 && trades.forall(trade => trade.accountNo == account.no && trade.isin == isin)
            ))
        }
      }
    }.provideCustomLayer(TradingServiceTest.layer)
  )
}

object TradingServiceTest {
  val serviceLayer: URLayer[
    AccountRepository with OrderRepository with ExecutionRepository with TradeRepository,
    TradingService
  ] = {
    (TradingServiceLive(_, _, _, _)).toLayer
  }
  val layer =
    (AccountRepositoryInMemory.layer ++
      OrderRepositoryInMemory.layer ++
      ExecutionRepositoryInMemory.layer ++
      TradeRepositoryInMemory.layer) >+> serviceLayer
}
