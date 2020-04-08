import client.IdentityClient
import org.scalatest.funsuite.AnyFunSuite

final class RMITest extends AnyFunSuite {
  test("--create option without real name") {
    IdentityClient(
      Array("-s", "localhost", "--create", "login", "--password", "foo")).run()
  }

  test("--create option with real name") {
    IdentityClient(Array("-s", "localhost", "--create", "login", "name",
      "--password", "foo")).run()
  }

  test("--delete option") {
    IdentityClient(
      Array("-s", "localhost", "--delete", "login", "--password", "foo")).run()
  }

  test("--modify option") {
    IdentityClient(
      Array("-s", "localhost", "--modify", "login", "new", "--password", "foo"))
      .run()
  }

  test("--get option with users") {
    IdentityClient(Array("-s", "localhost", "--get", "users")).run()
  }

  test("--get option with uuids") {
    IdentityClient(Array("-s", "localhost", "--get", "uuids")).run()
  }

  test("--get option with all") {
    IdentityClient(Array("-s", "localhost", "--get", "all")).run()
  }

  test("--reverse-lookup option") {
    IdentityClient(Array("-s", "localhost", "--reverse-lookup", "uuid")).run()
  }

  test("--lookup option") {
    IdentityClient(Array("-s", "localhost", "--lookup", "username")).run()
  }
}
