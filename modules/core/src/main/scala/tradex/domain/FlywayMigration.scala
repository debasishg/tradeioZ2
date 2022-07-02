package tradex.domain

import org.flywaydb.core.Flyway
import zio.Task
import config._
import zio.ZIO

object FlywayMigration {
  def migrate(config: DBConfig): Task[Unit] =
    ZIO
      .attemptBlocking(
        Flyway
          .configure(this.getClass.getClassLoader)
          .dataSource(config.url, config.user, config.password)
          .locations("migrations")
          .connectRetries(Int.MaxValue)
          .load()
          .migrate()
      )
      .unit
}
