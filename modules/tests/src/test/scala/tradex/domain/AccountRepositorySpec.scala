package tradex.domain

import zio.test._
import zio.test.Assertion._

import generators._
import model.account._
import repository.AccountRepository
import repository.inmemory.AccountRepositoryInMemory
import zio.ZIO

object AccountRepositorySpec extends ZIOSpecDefault {
  val testLayer = AccountRepositoryInMemory.layer ++ TestRandom.deterministic ++ Sized.default ++ TestConfig.default

  val spec = suite("AccountRepository")(
    test("successfully stores an account") {
      check(accountGen) { account =>
        for {
          repo   <- ZIO.service[AccountRepository]
          stored <- repo.store(account)
        } yield assert(stored.accountType)(
          isOneOf(AccountType.values)
        )
      }
    }.provide(testLayer),
    test("successfully stores an account and fetch the same") {
      check(accountGen) { account =>
        for {
          repo    <- ZIO.service[AccountRepository]
          stored  <- repo.store(account)
          fetched <- repo.queryByAccountNo(stored.no)
        } yield assertTrue(stored.no == fetched.get.no)
      }
    }.provide(testLayer),
    test("successfully stores multiple accounts") {
      check(Gen.listOfN(5)(accountWithNamePatternGen("debasish"))) { accounts =>
        for {
          repo        <- ZIO.service[AccountRepository]
          _           <- repo.store(accounts)
          allAccounts <- repo.all
        } yield assertTrue(allAccounts.forall(_.name.value.value.startsWith("debasish")))
      }
    }.provide(testLayer)
  )
}
