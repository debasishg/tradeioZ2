package tradex.domain
package repository.doobie

import java.util.UUID
import java.time.{ LocalDate, LocalDateTime }
import zio._
import zio.prelude._
import zio.interop.catz._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._

import squants.market._

import model.account._
import model.instrument._
import model.order._
import model.user._
import model.trade._
import model.market._
import codecs._
import repository.TradeRepository
import config._

final case class TradeRepositoryLive(xa: Transactor[Task]) extends TradeRepository {
  import TradeRepositoryLive.SQL

  implicit val TradeAssociative: Associative[Trade] =
    new Associative[Trade] {
      def combine(left: => Trade, right: => Trade): Trade =
        left.copy(taxFees = left.taxFees ++ right.taxFees)
    }

  def queryTradeByAccountNo(accountNo: AccountNo, date: LocalDate): Task[List[Trade]] =
    SQL
      .getTradesByAccountNo(accountNo.value.value, date)
      .to[List]
      .map(_.groupBy(_.tradeRefNo))
      .map {
        _.map { case (refNo, lis) =>
          lis.reduce((t1, t2) => Associative[Trade].combine(t1, t2)).copy(tradeRefNo = refNo)
        }.toList
      }
      .transact(xa)
      .orDie

  def queryTradeByMarket(market: Market): Task[List[Trade]] =
    SQL
      .getTradesByMarket(market.entryName)
      .to[List]
      .map(_.groupBy(_.tradeRefNo))
      .map {
        _.map { case (refNo, lis) =>
          lis.reduce((t1, t2) => Associative[Trade].combine(t1, t2)).copy(tradeRefNo = refNo)
        }.toList
      }
      .transact(xa)
      .orDie

  def queryTradesByISINCodes(
      forDate: LocalDate,
      forIsins: Set[ISINCode]
  ): Task[List[Trade]] = ???

  def allTrades: Task[List[Trade]] =
    SQL.getAllTrades
      .to[List]
      .map(_.groupBy(_.tradeRefNo))
      .map {
        _.map { case (refNo, lis) =>
          lis.reduce((t1, t2) => Associative[Trade].combine(t1, t2)).copy(tradeRefNo = refNo)
        }.toList
      }
      .transact(xa)
      .orDie

  def store(trd: Trade): Task[Trade] =
    SQL
      .insertTrade(trd)
      .transact(xa)
      .map(_ => trd)
      .orDie

  def storeNTrades(trades: NonEmptyList[Trade]): Task[Unit] =
    trades.forEach(trade => store(trade)).map(_ => ())
}

object TradeRepositoryLive extends CatzInterop {
  val layer: ZLayer[DBConfig, Throwable, TradeRepository] = {
    (for {
      cfg        <- ZIO.environmentWith[DBConfig](_.get).toManaged
      transactor <- mkTransactor(cfg)
    } yield new TradeRepositoryLive(transactor)).toLayer
  }

  object SQL {

    // when writing we have a valid `Execution` - hence we can use
    // Scala data types
    implicit val tradeWrite: Write[Trade] =
      Write[
        (
            AccountNo,
            ISINCode,
            Market,
            BuySell,
            UnitPrice,
            Quantity,
            LocalDateTime,
            Option[LocalDateTime],
            Option[Money],
            Option[UserId]
        )
      ].contramap(trade =>
        (
          trade.accountNo,
          trade.isin,
          trade.market,
          trade.buySell,
          trade.unitPrice,
          trade.quantity,
          trade.tradeDate,
          trade.valueDate,
          trade.netAmount,
          trade.userId
        )
      )

    implicit val tradeTaxFeeWrite: Write[TradeTaxFee] =
      Write[
        (TaxFeeId, Money)
      ].contramap(tradeTaxFee => (tradeTaxFee.taxFeeId, tradeTaxFee.amount))

    implicit val tradeTaxFeeRead: Read[Trade] =
      Read[
        (
            TradeReferenceNo,
            AccountNo,
            ISINCode,
            Market,
            BuySell,
            UnitPrice,
            Quantity,
            LocalDateTime,
            Option[LocalDateTime],
            Option[Money],
            Option[UserId],
            TaxFeeId,
            Money
        )
      ].map { case (refNo, ano, isin, mkt, bs, up, qty, td, vd, netAmt, uid, tfid, amt) =>
        Trade(ano, isin, mkt, bs, up, qty, td, vd, uid, List(TradeTaxFee(tfid, amt)), netAmt, Some(refNo))
      }

    def insertTaxFees(refNo: TradeReferenceNo, taxFees: List[TradeTaxFee]): ConnectionIO[Int] = {
      val sql = s"""
        INSERT INTO tradeTaxFees 
				(
					tradeRefNo,
					taxFeeId,
					amount
				) VALUES (${refNo}, ?, ?)
			"""
      Update[TradeTaxFee](sql).updateMany(taxFees)
    }

    def insertTrade(trade: Trade): ConnectionIO[Int] = {
      val sql = sql"""
			  INSERT INTO trades
				(
          accountNo,
          isinCode,
          market,
          buySellFlag,
          unitPrice,
          quantity,
          tradeDate,
          valueDate,
          netAmount,
          userId
				) VALUES (
					${trade.accountNo},
					${trade.isin},
					${trade.market},
					${trade.buySell},
					${trade.unitPrice},
					${trade.quantity},
					${trade.tradeDate},
					${trade.valueDate},
					${trade.netAmount},
					${trade.userId}
				)
      """
      sql.update
        .withUniqueGeneratedKeys[UUID]("tradeRefNo")
        .flatMap { refNo =>
          insertTaxFees(TradeReferenceNo(refNo), trade.taxFees)
        }
    }

    def getTradesByAccountNo(ano: String, date: LocalDate) =
      sql"""
        SELECT t.tradeRefNo, t.accountNo, t.isinCode, t.market, t.buySellFlag, t.unitPrice, t.quantity,
               t.tradeDate, t.valueDate, t.netAmount, t.userId, f.taxFeeId, f.amount
        FROM   trades t, tradeTaxFees f
        WHERE  t.tradeRefNo = f.tradeRefNo
        AND    t.accountNo = $ano
        AND    DATE(t.tradeDate) = $date
      """.query[Trade]

    def getTradesByMarket(market: String) =
      sql"""
        SELECT t.tradeRefNo, t.accountNo, t.isinCode, t.market, t.buySellFlag, t.unitPrice, t.quantity,
               t.tradeDate, t.valueDate, t.netAmount, t.userId, f.taxFeeId, f.amount
        FROM   trades t, tradeTaxFees f
        WHERE  t.tradeRefNo = f.tradeRefNo
        AND    t.market = $market
      """.query[Trade]

    def getAllTrades =
      sql"""
        SELECT t.tradeRefNo, t.accountNo, t.isinCode, t.market, t.buySellFlag, t.unitPrice, t.quantity,
               t.tradeDate, t.valueDate, t.netAmount, t.userId, f.taxFeeId, f.amount
        FROM   trades t, tradeTaxFees f
        WHERE  t.tradeRefNo = f.tradeRefNo
      """.query[Trade]
  }
}
