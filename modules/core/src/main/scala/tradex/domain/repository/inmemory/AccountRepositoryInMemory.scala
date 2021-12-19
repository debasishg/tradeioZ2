package tradex.domain
package repository.inmemory

import zio._
import java.time._
import collection.immutable.Map
import repository.AccountRepository
import model.account._

final case class AccountRepositoryInMemory(state: Ref[Map[AccountNo, Account]]) extends AccountRepository {

  override def queryByAccountNo(no: AccountNo): Task[Option[Account]] = state.get.map(_.get(no))

  override def store(a: Account): Task[Account] = state.update(m => m + ((a.no, a))).map(_ => a)

  override def store(as: List[Account]): Task[Unit] = state.update(m => m ++ (as.map(a => (a.no, a))))

  override def allOpenedOn(openedOnDate: LocalDate): Task[List[Account]] =
    state.get.map(_.values.filter(_.dateOfOpen == openedOnDate).toList)

  override def all: Task[List[Account]] = state.get.map(_.values.toList)

  override def allClosed(closeDate: Option[LocalDate]): Task[List[Account]] =
    state.get.map(_.values.filter(_.dateOfClose.isDefined).toList)

  override def allAccountsOfType(accountType: AccountType): Task[List[Account]] =
    state.get.map(_.values.filter(_.accountType == accountType).toList)
}

object AccountRepositoryInMemory {
  val layer: ULayer[AccountRepository] =
    Ref.make(Map.empty[AccountNo, Account]).map(r => AccountRepositoryInMemory(r)).toLayer
}
