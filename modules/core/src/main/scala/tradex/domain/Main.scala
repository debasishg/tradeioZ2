package tradex.domain

import zio._
import zio.prelude.NonEmptyList

import services.trading.TradingService
import services.accounting.AccountingService
import java.time._
import model.trade._
import model.user._
import model.balance._

object Main extends ZIOApp {
  override type Environment = config.Config with TradingService with AccountingService

  override implicit def tag: Tag[Environment] = Tag[Environment]

  override def layer: ZLayer[ZIOAppArgs, Any, Environment] = Application.prod.appLayer

  override def run: ZIO[config.Config with TradingService, Throwable, Unit] = {
    val setupDB: ZIO[config.Config, Throwable, Unit] = for {
      dbConf <- config.getDbConfig
      _      <- FlywayMigration.migrate(dbConf)
    } yield ()

    ZIO.serviceWithZIO[config.Config with TradingService](service =>
      setupDB *> service.getAccountsOpenedOn(LocalDate.EPOCH).map(_.foreach(println))
    )
  }
}

object Program {
  def generate(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): ZIO[TradingService with AccountingService, Throwable, (NonEmptyList[Trade], NonEmptyList[Balance])] = {

    ZIO
      .serviceWithZIO[TradingService with AccountingService](service =>
        for {
          trades   <- service.generateTrade(frontOfficeInput, userId)
          balances <- service.postBalance(trades)
        } yield (trades, balances)
      )
  }
}
