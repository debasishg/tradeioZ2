package tradex.domain

import zio._

import config._
import services.trading._
import services.accounting._
import repository._
import repository.inmemory._

object Application {

  type RepositoryLayerEnv =
    AccountRepository with OrderRepository with ExecutionRepository with TradeRepository with BalanceRepository

  type ServiceLayerEnv =
    TradingService with AccountingService

  type AppEnv = ServiceLayerEnv

  // compose layers for prod
  object prod {
    val configLayer: ZLayer[Any, IllegalStateException, Config] = ConfigProvider.live

    val repositoryLayer: URLayer[
      Any,
      RepositoryLayerEnv,
    ] =
      AccountRepositoryInMemory.layer ++ OrderRepositoryInMemory.layer ++ ExecutionRepositoryInMemory.layer ++ TradeRepositoryInMemory.layer ++ BalanceRepositoryInMemory.layer // ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      TradingServiceLive.layer ++ AccountingServiceLive.layer

    // final application layer for prod
    val appLayer = // : ZLayer[Any, Throwable, AppEnv] =
      configLayer >+>
        repositoryLayer >+>
        serviceLayer
  }
}
