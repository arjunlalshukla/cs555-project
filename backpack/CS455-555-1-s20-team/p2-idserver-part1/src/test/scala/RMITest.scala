import java.util.concurrent.TimeUnit

import org.scalatest.funsuite.AnyFunSuite
import client.IdentityClient
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.IdentityServer
import os._

final class RMITerminatedException(ecode: Int) extends
  Exception(s"RMI registry terminated with code $ecode")

final class RMITest extends AnyFunSuite with BeforeAndAfterAll
  with BeforeAndAfter {

  private[this] var rmiProc: Process = _

  override def beforeAll(): Unit = {
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

  override def afterAll(): Unit = {
    rmiProc.destroy()
    if (!rmiProc.waitFor(3, TimeUnit.SECONDS)) {
      rmiProc.destroyForcibly()
      rmiProc.waitFor()
    }
  }

  test("--create option without real name") {
    IdentityClient.main(
      Array("-s", "localhost", "--create", "login", "--password", "foo"))
  }

  test("--create option with real name") {
    IdentityClient.main(Array("-s", "localhost", "--create", "login", "name",
      "--password", "foo"))
  }

  test("--delete option") {
    IdentityClient.main(
      Array("-s", "localhost", "--delete", "login", "--password", "foo"))
  }

  test("--modify option") {
    IdentityClient.main(
      Array("-s", "localhost", "--modify", "login", "new", "--password", "foo"))
  }

  test("--get option with users") {
    IdentityClient.main(Array("-s", "localhost", "--get", "users"))
  }

  test("--get option with uuids") {
    IdentityClient.main(Array("-s", "localhost", "--get", "uuids"))
  }

  test("--get option with all") {
    IdentityClient.main(Array("-s", "localhost", "--get", "all"))
  }

  test("--reverse-lookup option") {
    IdentityClient.main(Array("-s", "localhost", "--reverse-lookup", "uuid"))
  }

  test("--lookup option") {
    IdentityClient.main(Array("-s", "localhost", "--lookup", "username"))
  }
}
