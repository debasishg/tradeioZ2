package tradex.domain
package repository.doobie

import java.util.UUID
import zio._
import zio.interop.catz._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import model.user._
import codecs._
import config._
import repository.UserRepository

final case class UserRepositoryLive(xa: Transactor[Task]) extends UserRepository {
  import UserRepositoryLive.SQL

  def queryByUserName(username: UserName): Task[Option[User]] =
    SQL
      .getByUserName(username.value.value)
      .option
      .transact(xa)
      .orDie

  def store(username: UserName, password: EncryptedPassword): Task[UserId] =
    SQL
      .insert(username, password)
      .transact(xa)
      .map(_.userId)
      .orDie
}

object UserRepositoryLive extends CatzInterop {
  val layer: ZLayer[DBConfig, Throwable, UserRepository] = {
    (for {
      cfg        <- ZIO.environmentWith[DBConfig](_.get).toManaged
      transactor <- mkTransactor(cfg)
    } yield new UserRepositoryLive(transactor)).toLayer
  }

  object SQL {
    def insert(userName: UserName, password: EncryptedPassword): ConnectionIO[User] =
      sql"""
        INSERT INTO users (name, password)
        VALUES ($userName, $password)
      """.update
        .withUniqueGeneratedKeys("id")

    // when writing we have a valid `User` - hence we can use
    // Scala data types
    implicit val accountWrite: Write[User] =
      Write[
        (
            UserId,
            UserName,
            EncryptedPassword
        )
      ].contramap(user =>
        (
          user.userId,
          user.userName,
          user.password
        )
      )

    // when reading we can encounter invalid Scala types since
    // data might have been inserted into the database external to the
    // application. Hence we use raw types and a smart constructor that
    // validates the data types
    implicit val userRead: Read[User] =
      Read[
        (
            UUID,
            String,
            String
        )
      ].map { case (uuid, name, pass) =>
        User
          .user(uuid, name, pass)
          .fold(exs => throw new Exception(exs.toList.mkString("/")), identity)
      }

    def getByUserName(name: String): Query0[User] = sql"""
      select * from users where name = $name
      """.query[User]
  }
}
