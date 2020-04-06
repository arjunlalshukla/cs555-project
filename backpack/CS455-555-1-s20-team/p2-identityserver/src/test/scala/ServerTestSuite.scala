import org.scalatest.funsuite.AnyFunSuite
class ServerTestSuite extends AnyFunSuite {
  test("Should be able to add a user with a password"){

    assert(IdentityServer.create())
  }
}
