package tradex.domain

import pureconfig.{ ConfigConvert, ConfigSource }
import pureconfig.generic.semiauto._
import zio._

object config {
  final case class Config(
      appConfig: AppConfig,
      dbConfig: DBConfig
  )

  object Config {
    implicit val convert: ConfigConvert[Config] = deriveConvert
  }

  object ConfigProvider {

    val live: ZLayer[Any, IllegalStateException, Config] =
      ZLayer.fromZIO {
        ZIO
          .fromEither(ConfigSource.default.load[Config])
          .mapError(failures =>
            new IllegalStateException(
              s"Error loading configuration: $failures"
            )
          )
      }
  }

  object DbConfigProvider {

    val fromConfig: ZLayer[Config, Nothing, DBConfig] =
      ZLayer.fromFunction(_.get[Config].dbConfig)
  }

  object AppConfigProvider {

    val fromConfig: ZLayer[Config, Nothing, AppConfig] =
      ZLayer.fromFunction(_.get[Config].appConfig)
  }

  // accessor to get dbconfig out
  def getDbConfig: ZIO[Config, Throwable, DBConfig] =
    ZIO.environmentWith(_.get[Config].dbConfig)

  // accessor to get appconfig out
  def getAppConfig: ZIO[Config, Throwable, AppConfig] =
    ZIO.environmentWith(_.get[Config].appConfig)

  case class AppConfig(
      maxAccountNoLength: Int,
      minAccountNoLength: Int,
      zeroBalanceAllowed: Boolean
  )

  object AppConfig {
    implicit val convert: ConfigConvert[AppConfig] = deriveConvert
  }

  final case class DBConfig(
      url: String,
      driver: String,
      user: String,
      password: String
  )
  object DBConfig {
    implicit val convert: ConfigConvert[DBConfig] = deriveConvert
  }
}
