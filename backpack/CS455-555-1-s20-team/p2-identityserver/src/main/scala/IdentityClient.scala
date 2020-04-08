import picocli.CommandLine
import java.rmi.registry.LocateRegistry.getRegistry

object IdentityClient {

  def main(args: Array[String]): Unit = {
    System.setProperty("javax.net.ssl.trustStore", "Client_Truststore")
    System.setProperty("java.security.policy", "mysecurity.policy")
    System.setProperty("javax.net.ssl.trustStorePassword", "test123")
    IdentityClient(args).run()
  }

  def apply(args: Array[String]): IdentityClient = {
    val idc = new IdentityClient
    val res = new CommandLine(idc).parseArgs(args: _*).errors
    if (!res.isEmpty) {
      throw res.get(0)
    }
    idc
  }
}

final class IdentityClient extends Runnable {
  @CommandLine.Option(names=Array("-s","--server"), required=true,
    description=Array("the identity server to connect to"))
  private[this] var server: String = _
  @CommandLine.Option(names=Array("-n", "--number"),
    description=Array("the port to connect on"))
  private[this] var port: Int = IdentityServer.rmiPort
  @CommandLine.Option(names=Array("-c", "--create"), arity="1..2",
    description=Array("create a new login; usable with --password"))
  private[this] var create: Array[String] = _
  @CommandLine.Option(names=Array("-d", "--delete"),
    description=Array("delete the account; password protected"))
  private[this] var delete: String = _
  @CommandLine.Option(names=Array("-m", "--modify"), arity="2",
    description=Array("change your login name; password protected"))
  private[this] var modify: Array[String] = _
  @CommandLine.Option(names=Array("-l", "--lookup"),
    description=Array("gives all information on the given user"))
  private[this] var lookup: String = _
  @CommandLine.Option(names=Array("-r", "--reverse-lookup"),
    description=Array("look up a user by UUID"))
  private[this] var rev_lookup: String = _
  @CommandLine.Option(names=Array("-g", "--get"),
    description=Array("retrieve information from the server"))
  private[this] var get: String = _
  @CommandLine.Option(names=Array("-p", "--password"),
    description=Array("provide a password for certain queries"))
  private[this] var password: String = _
  @CommandLine.Option(names=Array("-h", "--help"), usageHelp=true)
  private[this] var help: Boolean = false

  def run(): Unit = {
    if (help) {
      new CommandLine(this).usage(System.out)
      return
    }
    if (Seq(create,delete,modify,lookup,rev_lookup,get).count(_ != null) != 1) {
      throw new OneQueryExcpetion
    }
    lazy val stub = getRegistry(server, IdentityServer.rmiPort)
      .lookup("IdentityServer").asInstanceOf[IdentityServerInterface]
    if (password == null) {
      if (get != null) {
        get match {
          case "users" => stub.users.foreach(println)
          case "uuids" => stub.UUIDs.foreach(println)
          case "all" => stub.all.foreach(println)
          case _ => throw new GetOptionException
        }
      } else if (lookup != null) {
        println(stub.lookup(lookup))
      } else if (rev_lookup != null) {
        println(stub.reverse_lookup(rev_lookup))
      }
      else {
        throw new PasswordRequiredException
      }
    } else {
      if (create != null) {
        stub.create(create(0), create.lift(1).getOrElse(create(0)), pass_to_hash(password)) match {
          case null => println(s"Failed to create user ${create(0)}")
          case id: String => println(s"Created user ${create(0)} with id $id")
        }
      } else if (modify != null) {
        stub.modify(modify(0), pass_to_hash(password), modify(1)) match {
          case false => println("Failed to update user")
          case true => println(s"Successfully modified user ${modify(0)}")
        }
      } else if (delete != null) {
        stub.delete(delete, pass_to_hash(password)) match {
          case false => println(s"Removed user $delete")
          case true => println(s"Failed to remove user $delete")
        }
      } else {
        throw new PasswordIncompatibleException
      }
    }
  }

  def pass_to_hash(pass: String) : String = {
    String.format("%064x",
      new java.math.BigInteger(1,
        java.security.MessageDigest.getInstance("SHA-256").digest(pass.getBytes("UTF-8"))))
  }
}
