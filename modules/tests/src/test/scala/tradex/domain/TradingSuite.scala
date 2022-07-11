package tradex.domain

import zio.test._

object TradingSuite extends ZIOSpecDefault {
  val spec = (suite("Test Suite")(
    AccountRepositorySpec.spec,
    TradingServiceSpec.spec
  ))
}
