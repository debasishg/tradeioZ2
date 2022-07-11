package tradex.domain
package repository.inmemory

import zio._
import java.time._
import collection.immutable.Map
import repository.AccountRepository
import model.account._

final case class AccountRepositoryInMemory(state: Ref[Map[AccountNo, Account]]) extends AccountRepository {

  def queryByAccountNo(no: AccountNo): Task[Option[Account]] = state.get.map(_.get(no))

  def store(a: Account): Task[Account] = state.update(m => m + ((a.no, a))).map(_ => a)

  def store(as: List[Account]): Task[Unit] = state.update(m => m ++ (as.map(a => (a.no, a))))

  def allOpenedOn(openedOnDate: LocalDate): Task[List[Account]] =
    state.get.map(_.values.filter(_.dateOfOpen == openedOnDate).toList)

  def all: Task[List[Account]] = state.get.map(_.values.toList)

  def allClosed(closeDate: Option[LocalDate]): Task[List[Account]] =
    state.get.map(_.values.filter(_.dateOfClose.isDefined).toList)

  def allAccountsOfType(accountType: AccountType): Task[List[Account]] =
    state.get.map(_.values.filter(_.accountType == accountType).toList)

  def deleteAll: Task[Unit] =
    state.update(m => m -- m.keys)
}

object AccountRepositoryInMemory {
  val layer: ULayer[AccountRepository] =
    ZLayer.scoped(
      Ref.make(Map.empty[AccountNo, Account]).map(AccountRepositoryInMemory(_))
    )
}
