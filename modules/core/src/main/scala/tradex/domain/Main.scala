package tradex.domain

import zio._
import zio.prelude.NonEmptyList

import services.trading.{ TradingService, TradingServiceError }
import TradingService._
import TradingServiceError._
import services.accounting.AccountingService
import java.time._
import model.trade._
import model.user._
import model.balance._

object Main extends zio.ZIOApp {
  override type Environment = TradingService with AccountingService

  override implicit def tag: Tag[Environment] = Tag[Environment]

  override def layer: ZLayer[ZIOAppArgs, Any, Environment] = Application.prod.appLayer

  override def run: ZIO[TradingService, TradingError, Unit] = {
    ZIO.serviceWithZIO[TradingService](
      _.getAccountsOpenedOn(LocalDate.EPOCH).map(_.foreach(println))
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
