package tradex.domain

import zio.test._
import zio.test.Assertion._
import tradex.domain.services.trading.FrontOfficeTradeGenerationSpec
import tradex.domain.services.trading.TradingServiceSpec
import tradex.domain.repository.doobie.AccountRepositoryLive
import tradex.domain.repository.doobie.InstrumentRepositoryLive
import tradex.domain.services.trading.TradingServiceLive
import tradex.domain.repository.doobie.OrderRepositoryLive
import tradex.domain.repository.doobie.TradeRepositoryLive
import tradex.domain.repository.doobie.ExecutionRepositoryLive
import tradex.domain.config
import zio.config.syntax._

object ITSuite extends ZIOSpecDefault {
  val spec: Spec[TestEnvironment, Any] = suite("IT Suite")(
    FrontOfficeTradeGenerationSpec.spec,
    TradingServiceSpec.spec
  ).provide(
    TestRandom.deterministic,
    AccountRepositoryLive.layer,
    InstrumentRepositoryLive.layer,
    TradingServiceLive.layer,
    OrderRepositoryLive.layer,
    TradeRepositoryLive.layer,
    ExecutionRepositoryLive.layer,
    Sized.default,
    config.appConfig.narrow(_.dbConfig)
  )
}
