package tradex.domain
package repository.inmemory

import zio._
import model.instrument._
import repository.InstrumentRepository

final case class InstrumentRepositoryInMemory(state: Ref[Map[ISINCode, Instrument]]) extends InstrumentRepository {

  def queryByISINCode(isin: ISINCode): Task[Option[Instrument]] = state.get.map(_.get(isin))

  def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]] =
    state.get.map(_.values.filter(_.instrumentType == instrumentType).toList)

  def store(ins: Instrument): Task[Instrument] = state.update(m => m + ((ins.isinCode, ins))).map(_ => ins)
}

object InstrumentRepositoryInMemory {
  val layer: ULayer[InstrumentRepository] =
    ZLayer(
      Ref.make(Map.empty[ISINCode, Instrument]).map(r => InstrumentRepositoryInMemory(r))
    )
}
