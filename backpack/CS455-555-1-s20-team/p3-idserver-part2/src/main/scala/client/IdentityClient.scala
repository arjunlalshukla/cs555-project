package client

import java.rmi.registry.LocateRegistry.getRegistry

import picocli.CommandLine
import server.{
  AllReturn, CreateReturn, DeleteReturn, IAmNotTheCoor, IdentityServer,
  IdentityServerInterface, LookupReturn, ModifyReturn, RevLookReturn,
  ServerResponse, UUIDsReturn, UsersReturn
}

/**
 * client driver
 */
object IdentityClient {

  def main(args: Array[String]): Unit = {
    System.setProperty("javax.net.ssl.trustStore", "Client_Truststore")
    System.setProperty("java.security.policy", "mysecurity.policy")
    System.setProperty("javax.net.ssl.trustStorePassword", "test123")
    IdentityClient(args).run()
  }

  /**
   * setup the client
   * @param args args from command line
   * @return the client, all setup and ready to go
   */
  def apply(args: Array[String]): IdentityClient = {
    val idc = new IdentityClient
    val res = new CommandLine(idc).parseArgs(args: _*).errors
    if (!res.isEmpty) {
      throw res.get(0)
    }
    idc
  }
}

/**
 * Identity Client implementation
 */
final class IdentityClient extends Runnable {
  @CommandLine.Option(names=Array("-s","--server"), arity="1..*", required=true,
    description=Array("the identity server to connect to"))
  private[this] var server: Array[String] = _
  @CommandLine.Option(names=Array("-n", "--number"),
    description=Array("the port to connect on"))
  private[this] var port: Int = IdentityServer.clientRMIPort
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

  /**
   * overridden run method
   */
  def run(): Unit = {
    if (help) {
      new CommandLine(this).usage(System.out)
      return
    }
    if (Seq(create,delete,modify,lookup,rev_lookup,get).count(_ != null) != 1) {
      throw new OneQueryExcpetion
    }
    server.takeWhile { ip =>
      try {
        contactServer(ip)
        false
      } catch {
        case _: java.rmi.ConnectIOException |
             _: java.rmi.ConnectException => true
      }
    }
  }

  private[this] def contactServer(ip: String): Unit = {
    println(s"contacting server $ip")
    lazy val stub = getRegistry(ip, IdentityServer.clientRMIPort)
      .lookup("IdentityServer").asInstanceOf[IdentityServerInterface]
    processResponse(
      if (password == null) {
        if (get != null) {
          get match {
            case "users" => stub.users
            case "uuids" => stub.UUIDs
            case "all" => stub.all
            case _ => throw new GetOptionException
          }
        } else if (lookup != null) {
          stub.lookup(lookup)
        } else if (rev_lookup != null) {
          stub.reverse_lookup(rev_lookup)
        } else {
          throw new PasswordRequiredException
        }
      } else {
        if (create != null) {
          stub.create(
            create(0),
            create.lift(1).getOrElse(create(0)),
            pass_to_hash(password)
          )
        } else if (modify != null) {
          stub.modify(modify(0), pass_to_hash(password), modify(1))
        } else if (delete != null) {
          stub.delete(delete, pass_to_hash(password))
        } else {
          throw new PasswordIncompatibleException
        }
      }
    )
  }

  private[this] def processResponse(sr: ServerResponse): Unit = sr match {
    case IAmNotTheCoor(ip) => contactServer(ip)
    case AllReturn(vals) => vals.foreach(println)
    case UsersReturn(vals) => vals.foreach(println)
    case UUIDsReturn(vals) => vals.foreach(println)
    case LookupReturn(user) => println(user)
    case RevLookReturn(user) => println(user)
    case CreateReturn(None) => println(s"Failed to create user ${create(0)}")
    case CreateReturn(Some(id)) => println(s"Created user ${create(0)} with id $id")
    case ModifyReturn(true) => println(s"Successfully updated user ${modify(0)} to ${modify(1)}")
    case ModifyReturn(false) => println(s"Failed to update user ${modify(0)}")
    case DeleteReturn(true) => println(s"Removed user $delete")
    case DeleteReturn(false) => println(s"Failed to remove user $delete")
  }

  /**
   * convert a user's password to a hash for transmitting to the server
   * @param pass the user's password
   * @return the hashed value of the password
   */
  private[this] def pass_to_hash(pass: String) : String = {
    String.format("%064x",
      new java.math.BigInteger(1,
        java.security.MessageDigest.getInstance("SHA-256").digest(pass.getBytes("UTF-8"))))
  }
}
