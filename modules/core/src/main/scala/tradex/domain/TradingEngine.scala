package tradex.domain

import zio.ZIOAppDefault
import zio.ZIO
import zio.ZIOAppArgs
import zio.Scope
import tradex.domain.config._
import services.trading.TradingService
import java.time.LocalDate

object TradingEngine extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val setupDB = for {
      dbConf <- ZIO.serviceWith[AppConfig](_.dbConfig)
      _      <- FlywayMigration.migrate(dbConf)
    } yield ()

    ZIO
      .serviceWithZIO[TradingService](service =>
        setupDB *> service.getAccountsOpenedOn(LocalDate.EPOCH).map(_.foreach(println))
      )
      .provide(Application.prod.appLayer)
  }
}
