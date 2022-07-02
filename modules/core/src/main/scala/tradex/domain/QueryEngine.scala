package tradex.domain

import zio._
import model.instrument._
import zio.query.DataSource
import zio.query.Request
import tradex.domain.services.trading.TradingServiceError.TradingError
import tradex.domain.model.trade.Trade
import zio.query.ZQuery
import tradex.domain.services.trading.TradingService
import java.time.LocalDate
import eu.timepit.refined.api.RefType

object QueryEngine extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val appleISINStr = "US0378331005"
    val baeISINStr   = "GB0002634946"
    val ibmISINStr   = "US4592001014"

    val isins = List(appleISINStr, baeISINStr, ibmISINStr)
      .map(str =>
        RefType
          .applyRef[ISINCodeString](str)
          .map(ISINCode(_))
          .fold(err => throw new Exception(err), identity)
      )

    QueryProgram
      .queryTrades(LocalDate.now, isins.toSet)
      .run
      .provide(Application.prod.appLayer)
  }
}

object QueryProgram {
  def queryTrades(forDate: LocalDate, isins: Set[ISINCode]): ZQuery[TradingService, TradingError, List[Trade]] = {
    case class GetTradesByInstruments(date: LocalDate, isins: NonEmptyChunk[ISINCode])
        extends Request[TradingError, List[Trade]]

    val ds: DataSource[TradingService, GetTradesByInstruments] =
      DataSource.fromFunctionBatchedZIO(
        "TradesByInstruments"
      )(requests =>
        ZIO.foreach(requests)(request =>
          for {
            service <- ZIO.service[TradingService]
            r       <- service.getTradesByISINCodes(request.date, request.isins.toSet)
          } yield r
        )
      )

    ZQuery.fromRequest(GetTradesByInstruments(forDate, NonEmptyChunk(isins.head, isins.tail.toSeq: _*)))(ds)
  }
}
