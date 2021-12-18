package tradex.domain

import zio.prelude.NonEmptyList
import io.circe.{ Decoder, Encoder }
import squants.market.{ Currency, Money, USD }
import java.util.UUID

package object model extends OrphanInstances

// instances for types we don't control
trait OrphanInstances {
  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val currencyDecoder: Decoder[Currency] =
    Decoder[String].map(Currency.apply(_).get)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder[String].contramap(_.toString)

  implicit def nelDecoder[A: Decoder]: Decoder[NonEmptyList[A]] =
    Decoder[List[A]].map(l => NonEmptyList.apply(l.head, l.tail: _*))

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder[String].map(UUID.fromString(_))

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder[String].contramap(_.toString())

}
