import java.util.concurrent.TimeUnit

import org.scalatest.funsuite.AnyFunSuite
import client.IdentityClient
import org.scalatest.BeforeAndAfter
import server.IdentityServer
import os._

final class RMITerminatedException(ecode: Int) extends
  Exception(s"RMI registry terminated with code $ecode")

final class RMITest extends AnyFunSuite with BeforeAndAfter {

  private[this] var rmiProc: Process = _

  before {
    System.setProperty("javax.net.ssl.trustStore", "Client_Truststore")
    System.setProperty("java.security.policy", "mysecurity.policy")
    System.setProperty("javax.net.ssl.trustStorePassword", "test123")
    val pb = new ProcessBuilder
    pb.environment.put("CLASSPATH", (pwd/"target"/"scala-2.13"/"classes").toString +
      ":" + System.getenv("CLASSPATH"))
    rmiProc = pb.command("rmiregistry",  IdentityServer.rmiPort.toString).start()
    // wait for rmi to start up
    Thread.sleep(10000)
    try {
      throw new RMITerminatedException(rmiProc.exitValue)
    } catch { case _: IllegalThreadStateException =>
      new IdentityServer("IdentityServer").bind()
    }
  }

  after {
    rmiProc.destroy()
    if (!rmiProc.waitFor(3, TimeUnit.SECONDS)) {
      rmiProc.destroyForcibly()
      rmiProc.waitFor()
    }
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

  test("--reverse-lookup option") {
    IdentityClient(Array("-s", "localhost", "--reverse-lookup", "uuid")).run()
  }

  test("--lookup option") {
    IdentityClient(Array("-s", "localhost", "--lookup", "username")).run()
  }
}
