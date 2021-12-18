package tradex.domain
package repository

import zio._
import model.user._

trait UserRepository {

  /** query by username */
  def queryByUserName(username: UserName): Task[Option[User]]

  /** store a user * */
  def store(username: UserName, password: EncryptedPassword): Task[UserId]
}
