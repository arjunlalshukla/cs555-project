import org.scalatest.funsuite.AnyFunSuite
import picocli.CommandLine.{
  MissingParameterException,
  UnmatchedArgumentException
}

class CLISuite extends AnyFunSuite {
  test("-h option") {
    IdentityClient(Array("-h")).run()
  }

  test("--help option") {
    IdentityClient(Array("--help")).run()
  }

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

  test("--get option with invalid value") {
    assertThrows[GetOptionException] {
      IdentityClient(Array("-s", "localhost", "--get", "foo")).run()
    }
  }

  test("--reverse-lookup option") {
    IdentityClient(Array("-s", "localhost", "--reverse-lookup", "uuid")).run()
  }

  test("--lookup option") {
    IdentityClient(Array("-s", "localhost", "--lookup", "username")).run()
  }

  test("test server is required") {
    assertThrows[MissingParameterException] {
      IdentityClient(Array("-l", "hello")).run()
    }
  }

  test("test create requires password") {
    assertThrows[PasswordRequiredException] {
      IdentityClient(Array("-s", "localhost", "-c", "login", "name")).run()
    }
  }

  test("test delete requires password") {
    assertThrows[PasswordRequiredException] {
      IdentityClient(Array("-s", "localhost", "-d", "login")).run()
    }
  }

  test("test modify requires password") {
    assertThrows[PasswordRequiredException] {
      IdentityClient(Array("-s", "localhost", "-m", "login", "name")).run()
    }
  }

  test("test get rejects password") {
    assertThrows[PasswordIncompatibleException] {
      IdentityClient(Array("-s", "localhost", "--get", "users",
        "--password", "foo")).run()
    }
  }

  test("test lookup rejects password") {
    assertThrows[PasswordIncompatibleException] {
      IdentityClient(Array("-s", "localhost", "-l", "login", "--password", "foo")).run()
    }
  }

  test("test reverse lookup rejects password") {
    assertThrows[PasswordIncompatibleException] {
      IdentityClient(Array("-s", "localhost", "-r", "uuid", "--password", "foo")).run()
    }
  }

  test("we cannot have positional parameters") {
    assertThrows[UnmatchedArgumentException] {
      IdentityClient(Array("-s", "localhost", "foo")).run()
    }
  }
}
