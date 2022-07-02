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

import TradingServiceError._

trait TradingService {

  /** Find accounts opened on the date specified
    *
    * @param openDate
    *   the date when account was opened
    * @return
    *   a list of `Account` under the effect `IO`
    */
  def getAccountsOpenedOn(openDate: LocalDate): IO[TradingError, List[Account]]

  /** Find the list of trades for the supplied client account no and (optionally) the trade date.
    *
    * @param forAccountNo
    *   the client accountNo
    * @param forDate
    *   the trade date
    * @return
    *   a list of `Trade` under the effect `IO`
    */
  def getTrades(
      forAccountNo: AccountNo,
      forDate: Option[LocalDate] = None
  ): IO[TradingError, List[Trade]]

  /** Find the list of trades executed on the specified date and for the specified instruments.
    *
    * @param forDate
    *   the trade date
    * @param forIsins
    *   the instruments
    * @return
    *   a list of `Trade` under the effect `IO`
    */
  def getTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[model.instrument.ISINCode]
  ): IO[TradingError, List[Trade]]

  /** Create a list of `Order` from client orders that come from the front office.
    *
    * @param frontOfficeOrders
    *   client order
    * @return
    *   a NonEmptyList of `Order` under the effect `F`
    */
  def orders(
      frontOfficeOrders: NonEmptyList[FrontOfficeOrder]
  ): IO[TradingError, NonEmptyList[Order]]

  /** Execute an `Order` in the `Market` and book the execution in the broker account supplied.
    *
    * @param orders
    *   the orders to execute
    * @param market
    *   the market of execution
    * @param brokerAccount
    *   the broker account where the execution will be booked
    * @return
    *   a List of `Execution` generated from the `Order`
    */
  def execute(
      orders: NonEmptyList[Order],
      market: Market,
      brokerAccountNo: AccountNo
  ): IO[TradingError, NonEmptyList[Execution]]

  /** Allocate the `Execution` equally between the client accounts generating a list of `Trade`s.
    *
    * @param executions
    *   the executions to allocate
    * @param clientAccounts
    *   the client accounts for which `Trade` will be generated
    * @return
    *   a list of `Trade`
    */
  def allocate(
      executions: NonEmptyList[Execution],
      clientAccounts: NonEmptyList[AccountNo],
      userId: UserId
  ): IO[TradingError, NonEmptyList[Trade]]

  /** Workflow to generate trades from `frontOfficeInput`
    *
    * @param frontOfficeInput
    *   the front office input to generate trade from
    * @param userId
    *   the userid generating the trade
    * @return
    *   a list of `Trade`
    */
  final def generateTrade(
      frontOfficeInput: GenerateTradeFrontOfficeInput,
      userId: UserId
  ): IO[Throwable, NonEmptyList[Trade]] = {

    for {
      orders <- orders(frontOfficeInput.frontOfficeOrders)
      executions <- execute(
        orders,
        frontOfficeInput.market,
        frontOfficeInput.brokerAccountNo
      )
      trades <- allocate(executions, frontOfficeInput.clientAccountNos, userId)
    } yield trades
  }
}

object TradingServiceError {
  sealed trait TradingError extends Throwable {
    def cause: String
  }
  case class OrderingError(cause: String)        extends TradingError
  case class ExecutionError(cause: String)       extends TradingError
  case class AllocationError(cause: String)      extends TradingError
  case class TradeGenerationError(cause: String) extends TradingError
}
