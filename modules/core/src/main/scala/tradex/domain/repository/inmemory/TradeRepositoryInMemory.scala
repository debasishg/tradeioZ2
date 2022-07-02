package tradex.domain
package repository.inmemory

import zio._
import zio.prelude.NonEmptyList
import java.time._
import model.trade._
import model.account._
import model.market._
import model.instrument._
import repository.TradeRepository

final case class TradeRepositoryInMemory(state: Ref[Map[TradeReferenceNo, Trade]]) extends TradeRepository {

  def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): Task[List[Trade]] =
    state.get.map(_.values.filter(t => t.accountNo == accountNo && t.tradeDate.toLocalDate == date).toList)

  def queryTradeByMarket(market: Market): Task[List[Trade]] =
    state.get.map(_.values.filter(_.market == market).toList)

  def queryTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[ISINCode]
  ): Task[List[Trade]] = ???

  def allTrades: Task[List[Trade]] = state.get.map(_.values.toList)

  def store(trd: Trade): Task[Trade] =
    state
      .updateAndGet { m =>
        trd.tradeRefNo
          .map { refNo =>
            m + ((refNo, trd))
          }
          .getOrElse {
            val ref = TradeReferenceNo(java.util.UUID.randomUUID())
            m + ((ref, trd.copy(tradeRefNo = Some(ref))))
          }
      }
      .map(_ => trd)

  def storeNTrades(trades: NonEmptyList[Trade]): Task[Unit] = trades.forEach(t => store(t)).map(_ => ())
}

object TradeRepositoryInMemory {
  val layer: ULayer[TradeRepository] =
    ZLayer(
      Ref.make(Map.empty[TradeReferenceNo, Trade]).map(r => TradeRepositoryInMemory(r))
    )
}
