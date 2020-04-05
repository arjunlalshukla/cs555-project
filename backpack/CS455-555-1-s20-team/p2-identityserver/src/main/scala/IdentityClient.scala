import picocli.CommandLine

object IdentityClient {

  val defaultPort = 5190

  def main(args: Array[String]): Unit = {
    val f = IdentityClient(Array("-h"))
    f.run()
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
  private[this] var server: String = null
  @CommandLine.Option(names=Array("-n", "--number"),
    description=Array("the port to connect on"))
  private[this] var port: Int = IdentityClient.defaultPort
  @CommandLine.Option(names=Array("-c", "--create"), arity="1..2",
    description=Array("create a new login; usable with --password"))
  private[this] var create: Array[String] = null
  @CommandLine.Option(names=Array("-d", "--delete"),
    description=Array("delete the account; password protected"))
  private[this] var delete: String = null
  @CommandLine.Option(names=Array("-m", "--modify"), arity="2",
    description=Array("change your login name; password protected"))
  private[this] var modify: Array[String] = null
  @CommandLine.Option(names=Array("-l", "--lookup"),
    description=Array("gives all information on the given user"))
  private[this] var lookup: String = null
  @CommandLine.Option(names=Array("-r", "--reverse-lookup"),
    description=Array("look up a user by UUID"))
  private[this] var rev_lookup: String = null
  @CommandLine.Option(names=Array("-g", "--get"),
    description=Array("retrieve information from the server"))
  private[this] var get: String = null
  @CommandLine.Option(names=Array("-p", "--password"),
    description=Array("provide a password for certain queries"))
  private[this] var password: String = null
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
    if (password == null) {
      if (get != null) {
        if (!Set("users", "uuids", "all").contains(get)) {
          throw new GetOptionException
        }
      } else if (lookup != null) {}
      else if (rev_lookup != null) {}
      else {
        throw new PasswordRequiredException
      }
    } else {
      if (create != null) {}
      else if (modify != null) {}
      else if (delete != null) {}
      else {
        throw new PasswordIncompatibleException
      }
    }
  }
}
