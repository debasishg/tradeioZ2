package tradex.domain

import zio._
import zio.stream._
import zio.query.{ DataSource, Request, ZQuery }
import java.time.LocalDate
import model.instrument.ISINCode
import model.trade.Trade
import model.balance.Balance
import services.trading.{ TradingService, TradingServiceError }
import TradingServiceError._
import services.accounting.AccountingService

package object services {
  def queryTradesForDateForISINs(
      forDate: LocalDate,
      isins: Set[ISINCode]
  ): ZQuery[TradingService, TradingError, List[Trade]] = {

    case class GetTradesByInstruments(date: LocalDate, isins: NonEmptyChunk[ISINCode])
        extends Request[TradingError, List[Trade]]

    // the whole idea of using the `DataSource` is to abstract the batching
    // part and introduce some parallelism in the computation
    val ds: DataSource[TradingService, GetTradesByInstruments] =
      DataSource.fromFunctionBatchedM(
        "TradesByInstruments"
      )(requests =>
        ZIO
          .foreachPar(requests)(request =>
            ZIO.serviceWithZIO[TradingService](_.getTradesByISINCodes(request.date, request.isins.toSet))
          )
          .withParallelism(4)
      )

    ZQuery.fromRequest(GetTradesByInstruments(forDate, NonEmptyChunk(isins.head, isins.tail.toSeq: _*)))(ds)
  }

  def streamTradesForDateForISINs(
      forDate: LocalDate,
      isins: Set[ISINCode]
  ): ZStream[TradingService with AccountingService, Throwable, Balance] = {
    val trades = ZIO
      .serviceWithZIO[TradingService](
        _.getTradesByISINCodes(forDate, isins)
      )
      .mapError(_.getCause)

    ZStream
      .fromIterableZIO(trades)
      .mapZIOParUnordered(4)(trade => ZIO.serviceWithZIO[AccountingService](_.postBalance(trade)))
  }

}
