package tradex.domain
package model

import enumeratum._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.auto._
import io.estatico.newtype.macros.newtype
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

object market {
  sealed abstract class Market(override val entryName: String) extends EnumEntry

  object Market extends Enum[Market] {
    case object NewYork   extends Market("New York")
    case object Tokyo     extends Market("Tokyo")
    case object Singapore extends Market("Singapore")
    case object HongKong  extends Market("Hong Kong")
    case object Other     extends Market("Other")

    val values = findValues
    implicit val marketEncoder: Encoder[Market] =
      Encoder[String].contramap(_.entryName)

    implicit val marketDecoder: Decoder[Market] =
      Decoder[String].map(Market.withName(_))
  }

  @newtype
  case class MarketParam(value: NonEmptyString) {
    def toDomain: Market =
      Market.withName(value.value)
  }

  object MarketParam {
    implicit val jsonEncoder: Encoder[MarketParam] =
      Encoder.forProduct1("name")(_.value)

    implicit val jsonDecoder: Decoder[MarketParam] =
      Decoder.forProduct1("name")(MarketParam.apply)
  }
}
