package tradex.domain

import zio._
import zio.prelude.NonEmptyList

import services.trading.{ TradingService, TradingServiceError }
import TradingService._
import TradingServiceError._
import services.accounting.AccountingService
import AccountingService._
import java.time._
import model.trade._
import model.user._
import model.balance._
import model.instrument._
import zio.query.DataSource
import zio.query.Request
import zio.query.ZQuery

object Main extends zio.ZIOApp {
  override type Environment = TradingService with AccountingService

  override implicit def tag: Tag[Environment] = ???

  override def layer: ZLayer[ZIOAppArgs, Any, Environment] = Application.prod.appLayer

  override def run: ZIO[TradingService, TradingError, Unit] = {
    getAccountsOpenedOn(LocalDate.EPOCH).map(_.foreach(println))
  }
}

object Program {
  def generate(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): ZIO[Any, Throwable, (NonEmptyList[Trade], NonEmptyList[Balance])] = {
    (for {
      trades   <- generateTrade(frontOfficeInput, userId)
      balances <- postBalance(trades)
    } yield (trades, balances)).provideLayer(Application.prod.appLayer)
  }
}

object QueryProgram {
  def queryTrades(
      forDate: LocalDate,
      isins: Set[ISINCode]
  ): ZQuery[TradingService, TradingError, List[Trade]] = {
    case class GetTradesByInstruments(date: LocalDate, isins: NonEmptyChunk[ISINCode])
        extends Request[TradingError, List[Trade]]

    val ds: DataSource[TradingService, GetTradesByInstruments] =
      DataSource.fromFunctionBatchedM(
        "TradesByInstruments"
      )(requests => ZIO.foreach(requests)(request => getTradesByISINCodes(request.date, request.isins.toSet)))
    ZQuery.fromRequest(GetTradesByInstruments(forDate, NonEmptyChunk(isins.head, isins.tail.toSeq: _*)))(ds)
  }
}
