package tradex.domain
package model

import java.util.UUID
import scala.util.control.NoStackTrace
import zio.prelude._

import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import derevo.circe.magnolia._
import derevo.derive
import io.circe.refined._

import NewtypeRefinedOps._

object user {
  @derive(decoder, encoder)
  @newtype case class UserId(value: UUID)

  @derive(decoder, encoder)
  @newtype case class UserName(value: NonEmptyString)

  @derive(decoder, encoder)
  @newtype case class Password(value: NonEmptyString)

  @derive(decoder, encoder)
  @newtype case class EncryptedPassword(value: NonEmptyString)

  case class UserNotFound(username: UserName)    extends NoStackTrace
  case class UserNameInUse(username: UserName)   extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation               extends NoStackTrace
  case object TokenNotFound                      extends NoStackTrace

  @derive(decoder, encoder)
  private[domain] final case class User private (
      userId: UserId,
      userName: UserName,
      password: EncryptedPassword
  )

  object User {
    def user(
        id: UUID,
        name: String,
        password: String
    ): Validation[String, User] = {
      (
        validateUserName(name),
        validatePassword(password)
      ).mapN { (nm, pd) =>
        User(UserId(id), nm, pd)
      }
    }

    private[model] def validateUserName(
        name: String
    ): Validation[String, UserName] =
      validate[UserName](name)
        .mapError(s => s"User Name cannot be blank: (Root Cause: $s)")

    private[model] def validatePassword(
        name: String
    ): Validation[String, EncryptedPassword] =
      validate[EncryptedPassword](name)
        .mapError(s => s"User Password cannot be blank: (Root cause: $s)")
  }
}
