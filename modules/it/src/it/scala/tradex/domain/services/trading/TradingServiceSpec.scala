package tradex.domain.services.trading

import zio.ZIO
import zio.test._
import zio.test.Assertion._
import tradex.domain.generators
import tradex.domain.repository.AccountRepository
import tradex.domain.repository.InstrumentRepository
import java.util.UUID
import tradex.domain.model.user
import tradex.domain.model.instrument.InstrumentType.Equity
import tradex.domain.repository.TradeRepository
import tradex.domain.repository.OrderRepository
import tradex.domain.repository.ExecutionRepository

object TradingServiceSpec {
  def spec = suite("TradingServiceSpec")(
    test("add accounts and instruments")(check(generators.tradingAccountGen, generators.equityGen) {
      (account, equity) =>
        for {
          ar <- ZIO.service[AccountRepository]
          ir <- ZIO.service[InstrumentRepository]
          _  <- ar.store(account)
          _  <- ir.store(equity)
        } yield (assertTrue(true))
    }),
    test("generate trades from front office input") {
      (for {
        ar          <- ZIO.service[AccountRepository]
        ir          <- ZIO.service[InstrumentRepository]
        accounts    <- ar.all
        instruments <- ir.queryByInstrumentType(Equity)

      } yield generators.generateTradeFrontOfficeInputGenWithAccountAndInstrument(
        // accounts.map(_.no),
        // instruments.map(_.isinCode)
        List(accounts.head.no),
        List(instruments.head.isinCode)
      )).flatMap { gen =>
        check(gen) { foInput =>
          for {
            trading <- ZIO.service[TradingService]
            tr      <- ZIO.service[TradeRepository]
            trades  <- trading.generateTrade(foInput, user.UserId(UUID.randomUUID()))
            fetched <- tr.allTrades
          } yield (assertTrue(trades.size == fetched.size))
        }
      }
    } @@ TestAspect.sequential @@
      TestAspect.before(clean())
  )

  def clean() = for {
    ar <- ZIO.service[AccountRepository]
    ir <- ZIO.service[InstrumentRepository]
    or <- ZIO.service[OrderRepository]
    er <- ZIO.service[ExecutionRepository]
    tr <- ZIO.service[TradeRepository]
    _  <- tr.deleteAll
    _  <- er.deleteAll
    _  <- or.deleteAll
    _  <- ar.deleteAll
    _  <- ir.deleteAll
  } yield ()
}
