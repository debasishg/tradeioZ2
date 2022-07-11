package tradex.domain
package repository

import zio._
import model.instrument._

trait InstrumentRepository {

  /** query by account number */
  def queryByISINCode(isin: ISINCode): Task[Option[Instrument]]

  /** query by instrument type Equity / FI / CCY */
  def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]]

  /** store */
  def store(ins: Instrument): Task[Instrument]

  /** delete all instruments */
  def deleteAll: Task[Unit]
}

object InstrumentRepository {
  def queryByISINCode(isin: ISINCode): RIO[InstrumentRepository, Option[Instrument]] =
    ZIO.serviceWithZIO(_.queryByISINCode(isin))

  def queryByInstrumentType(instrumentType: InstrumentType): RIO[InstrumentRepository, List[Instrument]] =
    ZIO.serviceWithZIO(_.queryByInstrumentType(instrumentType))

  def store(ins: Instrument): RIO[InstrumentRepository, Instrument] =
    ZIO.serviceWithZIO(_.store(ins))
}
