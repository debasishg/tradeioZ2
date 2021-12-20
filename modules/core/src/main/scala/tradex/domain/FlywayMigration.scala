package tradex.domain

import org.flywaydb.core.Flyway
import zio.Task
import config._

object FlywayMigration {
  def migrate(config: DBConfig): Task[Unit] =
    Task {
      Flyway
        .configure(this.getClass.getClassLoader)
        .dataSource(config.url, config.user, config.password)
        .locations("migrations")
        .connectRetries(Int.MaxValue)
        .load()
        .migrate()
    }.unit
}
