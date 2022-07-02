package tradex.domain

import zio._

import tradex.domain.config
import services.trading._
import services.accounting._
import repository._
import repository.inmemory._
import tradex.domain.repository.doobie.AccountRepositoryLive
import tradex.domain.repository.doobie.OrderRepositoryLive
import tradex.domain.repository.doobie.ExecutionRepositoryLive
import tradex.domain.repository.doobie.TradeRepositoryLive
import tradex.domain.repository.doobie.BalanceRepositoryLive

object Application {

  type RepositoryLayerEnv =
    AccountRepository with OrderRepository with ExecutionRepository with TradeRepository with BalanceRepository

  type ServiceLayerEnv =
    TradingService with AccountingService

  type AppEnv = ServiceLayerEnv

  // compose layers for prod
  object prod {
    val configLayer = config.live

    val repositoryLayer =
      AccountRepositoryLive.layer ++ OrderRepositoryLive.layer ++ ExecutionRepositoryLive.layer ++ TradeRepositoryLive.layer ++ BalanceRepositoryLive.layer // ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      TradingServiceLive.layer ++ AccountingServiceLive.layer

    // final application layer for prod
    val appLayer = // : ZLayer[Any, Throwable, AppEnv] =
      configLayer >+>
        repositoryLayer >+>
        serviceLayer
  }
}
