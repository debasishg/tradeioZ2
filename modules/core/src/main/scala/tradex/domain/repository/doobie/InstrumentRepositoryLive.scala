package tradex.domain
package repository.doobie

import squants.market._
import zio._
import zio.interop.catz._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.instrument._
import model.order._
import tradex.domain.config._
import codecs._
import repository.InstrumentRepository
import cats.effect.kernel.Resource
import java.time.ZonedDateTime

final case class InstrumentRepositoryLive(xaResource: Resource[Task, Transactor[Task]]) extends InstrumentRepository {
  import InstrumentRepositoryLive.SQL

  def queryByISINCode(isin: ISINCode): Task[Option[Instrument]] =
    xaResource.use { xa =>
      SQL
        .get(isin.value.value)
        .option
        .transact(xa)
        .orDie
    }

  def queryByInstrumentType(instrumentType: InstrumentType): Task[List[Instrument]] =
    xaResource.use { xa =>
      SQL
        .getByType(instrumentType.entryName)
        .to[List]
        .transact(xa)
        .orDie
    }

  def store(ins: Instrument): Task[Instrument] =
    xaResource.use { xa =>
      SQL
        .upsert(ins)
        .run
        .transact(xa)
        .map(_ => ins)
        .orDie
    }

  def deleteAll: Task[Unit] =
    xaResource.use { xa =>
      SQL.deleteAll.run
        .transact(xa)
        .map(_ => ())
        .orDie
    }
}

object InstrumentRepositoryLive extends CatzInterop {
  val layer: ZLayer[DBConfig, Throwable, InstrumentRepository] = {
    ZLayer
      .scoped(for {
        cfg        <- ZIO.service[DBConfig]
        transactor <- mkTransactor(cfg)
      } yield new InstrumentRepositoryLive(transactor))
  }

  object SQL {
    def upsert(instrument: Instrument): Update0 = {
      sql"""
        INSERT INTO instruments
        VALUES (
          ${instrument.isinCode},
          ${instrument.name},
          ${instrument.instrumentType}::instrumentType,
          ${instrument.dateOfIssue},
          ${instrument.dateOfMaturity},
          ${instrument.lotSize},
          ${instrument.unitPrice},
          ${instrument.couponRate},
          ${instrument.couponFrequency}
        )
        ON CONFLICT(isinCode) DO UPDATE SET
          name                 = EXCLUDED.name,
          type                 = EXCLUDED.type,
          dateOfIssue          = EXCLUDED.dateOfIssue,
          dateOfMaturity       = EXCLUDED.dateOfMaturity,
          lotSize              = EXCLUDED.lotSize,
          unitPrice            = EXCLUDED.unitPrice,
          couponRate           = EXCLUDED.couponRate,
          couponFrequency      = EXCLUDED.couponFrequency
       """.update
    }

    // when writing we have a valid `Instrument` - hence we can use
    // Scala data types
    implicit val instrumentWrite: Write[Instrument] =
      Write[
        (
            ISINCode,
            InstrumentName,
            InstrumentType,
            Option[ZonedDateTime],
            Option[ZonedDateTime],
            LotSize,
            Option[UnitPrice],
            Option[Money],
            Option[BigDecimal]
        )
      ].contramap(instrument =>
        (
          instrument.isinCode,
          instrument.name,
          instrument.instrumentType,
          instrument.dateOfIssue,
          instrument.dateOfMaturity,
          instrument.lotSize,
          instrument.unitPrice,
          instrument.couponRate,
          instrument.couponFrequency
        )
      )

    // when reading we can encounter invalid Scala types since
    // data might have been inserted into the database external to the
    // application. Hence we use raw types and a smart constructor that
    // validates the data types
    implicit val instrumentRead: Read[Instrument] =
      Read[
        (
            String,
            String,
            String,
            Option[ZonedDateTime],
            Option[ZonedDateTime],
            Int,
            Option[BigDecimal],
            Option[BigDecimal],
            Option[BigDecimal]
        )
      ].map { case (isin, nm, insType, issueDt, matDt, lot, up, cpnRt, cpnFreq) =>
        Instrument
          .instrument(
            isin,
            nm,
            InstrumentType.withName(insType),
            issueDt,
            matDt,
            Some(lot),
            up,
            cpnRt.map(m => USD(m)),
            cpnFreq
          )
          .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
      }

    def get(isin: String): Query0[Instrument] = sql"""
      select * from instruments where isinCode = $isin
      """.query[Instrument]

    def getByType(instrumentType: String): Query0[Instrument] = sql"""
      select * from instruments where type = $instrumentType::instrumentType
      """.query[Instrument]

    def deleteAll: Update0 = sql"""
      delete from instruments
      """.update
  }
}
