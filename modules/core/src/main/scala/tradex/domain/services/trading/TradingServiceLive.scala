package tradex.domain
package services.trading

import zio._
import zio.prelude._
import java.time.LocalDate
import model.account._
import model.trade._
import model.order._
import model.market._
import model.execution._
import model.user._
import repository._
import NewtypeRefinedOps._
import TradingServiceError._

final case class TradingServiceLive(
    ar: AccountRepository,
    or: OrderRepository,
    er: ExecutionRepository,
    tr: TradeRepository
) extends TradingService {

  def getAccountsOpenedOn(openDate: LocalDate): IO[TradingError, List[Account]] =
    withTradeRepositoryService(ar.allOpenedOn(openDate))

  def getTrades(
      forAccountNo: AccountNo,
      forDate: Option[LocalDate] = None
  ): IO[TradingError, List[Trade]] = {
    withTradeRepositoryService(
      tr.queryTradeByAccountNo(
        forAccountNo,
        forDate.getOrElse(today.toLocalDate())
      )
    )
  }

  def getTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[model.instrument.ISINCode]
  ): IO[TradingError, List[Trade]] = {
    withTradeRepositoryService(
      tr.queryTradesByISINCodes(
        forDate,
        forIsins
      )
    )
  }

  def orders(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
  ): IO[TradingError, NonEmptyList[Order]] = {
    withTradeRepositoryService(
      Order
        .create(frontOfficeOrders)
        .fold(
          errs => ZIO.fail(OrderingError(errs.toList.mkString("/"))),
          orders => {
            NonEmptyList
              .fromIterableOption(orders)
              .map(os => (persistOrders(os) *> ZIO.succeed(os)))
              .getOrElse(ZIO.fail(OrderingError("Empty order list encountered")))
          }
        )
    )
  }

  def execute(
      orders: NonEmptyList[Order],
      market: Market,
      brokerAccountNo: AccountNo
  ): IO[TradingError, NonEmptyList[Execution]] = {
    val ois: NonEmptyList[(Order, model.order.LineItem)] = for {
      order <- orders
      item  <- order.items
    } yield (order, item)

    withTradeRepositoryService {
      for {
        executions <- ois.forEach { case (order, lineItem) =>
          ZIO.succeed(
            Execution.execution(
              brokerAccountNo,
              order.no,
              lineItem.instrument,
              market,
              lineItem.buySell,
              lineItem.unitPrice,
              lineItem.quantity,
              today
            )
          )
        }
        _ <- persistExecutions(executions)
      } yield executions
    }
  }

  def allocate(
      executions: NonEmptyList[Execution],
      clientAccounts: NonEmptyList[AccountNo],
      userId: UserId
  ): IO[TradingError, NonEmptyList[Trade]] = {
    val anoExes: NonEmptyList[(AccountNo, Execution)] = for {
      execution <- executions
      accountNo <- clientAccounts
    } yield (accountNo, execution)

    withTradeRepositoryService {
      for {
        tradesNoTaxFee <- anoExes.forEach { case (accountNo, execution) =>
          val q = execution.quantity.value.value / clientAccounts.size
          val qty = validate[Quantity](q)
            .fold(errs => throw new Exception(errs.toString), identity)

          Trade
            .trade(
              accountNo,
              execution.isin,
              execution.market,
              execution.buySell,
              execution.unitPrice,
              qty,
              execution.dateOfExecution,
              None,
              userId = Some(userId)
            )
            .fold(errs => ZIO.fail(AllocationError(errs.toList.mkString("/"))), ZIO.succeed(_))
        }
        tradesWithTaxFee = tradesNoTaxFee.map(t => Trade.withTaxFee(t))
        _         <- persistTrades(tradesWithTaxFee)
        allTrades <- tr.allTrades

      } yield NonEmptyList.fromIterable(
        allTrades.head,
        allTrades.tail
      )
    }
  }

  private def withTradeRepositoryService[A](t: Task[A]): IO[TradingError, A] =
    t.mapError(th => TradeGenerationError(th.getMessage))

  private def persistOrders(orders: NonEmptyList[Order]): IO[OrderingError, Unit] =
    or.store(orders)
      .mapError(th => OrderingError(th.getMessage))

  private def persistExecutions(executions: NonEmptyList[Execution]): IO[ExecutionError, Unit] =
    er.storeMany(executions)
      .mapError(th => ExecutionError(th.getMessage))

  private def persistTrades(trades: NonEmptyList[Trade]): IO[TradingError, Unit] =
    tr.storeNTrades(trades)
      .mapError(th => TradeGenerationError(th.getMessage))
}

object TradingServiceLive {
  val layer = ZLayer.fromFunction(TradingServiceLive.apply _)
}
