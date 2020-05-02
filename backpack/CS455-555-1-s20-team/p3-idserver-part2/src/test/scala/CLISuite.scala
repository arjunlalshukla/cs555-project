import client.{
  GetOptionException,
  PasswordIncompatibleException,
  PasswordRequiredException,
  IdentityClient
}
import org.scalatest.funsuite.AnyFunSuite
import picocli.CommandLine.{MissingParameterException, UnmatchedArgumentException}

class CLISuite extends AnyFunSuite {
  test("-h option") {
    IdentityClient(Array("-h")).run()
  }

  test("--help option") {
    IdentityClient(Array("--help")).run()
  }

  test("--get option with invalid value") {
    assertThrows[GetOptionException] {
      IdentityClient(Array("-s", "localhost", "--get", "foo")).run()
    }
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

  test("test accepts list of server") {
    assertThrows[java.rmi.ConnectException] {
      IdentityClient(Array("-s", "1.1.1.1", "1.1.1.2", "-g", "all")).run()
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
