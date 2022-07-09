package tradex.domain

import zio.config.magnolia.descriptor
import zio.TaskLayer
import zio.config.ConfigDescriptor._
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfig

object config {
  final case class AppConfig(
      tradingConfig: TradingConfig,
      dbConfig: DBConfig,
      dataSourceConfig: DataSourceConfig
  )

  final case class TradingConfig(
      maxAccountNoLength: Int,
      minAccountNoLength: Int,
      zeroBalanceAllowed: Boolean
  )

  final case class DBConfig(
      url: String,
      driver: String,
      user: String,
      password: String
  )

  final case class DataSourceConfig(
      conectionPoolSize: Int
  )

  type AllConfig = AppConfig with TradingConfig with DBConfig with DataSourceConfig

  final val Root = "tradex"

  private final val Descriptor = descriptor[AppConfig]

  val appConfig = TypesafeConfig.fromResourcePath(nested(Root)(Descriptor))

  val live: TaskLayer[AllConfig] = appConfig >+>
    appConfig.narrow(_.dbConfig) >+>
    appConfig.narrow(_.dataSourceConfig) >+>
    appConfig.narrow(_.tradingConfig)
}
