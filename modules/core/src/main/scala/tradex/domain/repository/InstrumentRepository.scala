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
}
