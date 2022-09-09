package tradex.domain

import zio._

import tradex.domain.config
import services.trading._
import services.accounting._
import repository._
import tradex.domain.repository.doobie.{
  AccountRepositoryLive,
  BalanceRepositoryLive,
  ExecutionRepositoryLive,
  OrderRepositoryLive,
  TradeRepositoryLive
}
import zio.logging.backend.SLF4J

object Application {

  type RepositoryLayerEnv =
    AccountRepository with OrderRepository with ExecutionRepository with TradeRepository with BalanceRepository

  type ServiceLayerEnv =
    TradingService with AccountingService

  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  // compose layers for prod
  object prod {
    val configLayer = config.live ++ logger

    val repositoryLayer =
      AccountRepositoryLive.layer ++ OrderRepositoryLive.layer ++ ExecutionRepositoryLive.layer ++ TradeRepositoryLive.layer ++ BalanceRepositoryLive.layer // ++ ZLayer.identity

    val serviceLayer: ZLayer[RepositoryLayerEnv, Throwable, ServiceLayerEnv] =
      TradingServiceLive.layer ++ AccountingServiceLive.layer

    // final application layer for prod
    val appLayer =
      configLayer >+>
        repositoryLayer >+>
        serviceLayer
  }
}
