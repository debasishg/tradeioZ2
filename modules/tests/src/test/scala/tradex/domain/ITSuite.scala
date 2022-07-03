package tradex.domain

import zio.test._

object ITSuite extends ZIOSpecDefault {
  val spec = (suite("Integration Suite")(
    AccountRepositorySpec.spec,
    TradingServiceSpec.spec
  ))
}
