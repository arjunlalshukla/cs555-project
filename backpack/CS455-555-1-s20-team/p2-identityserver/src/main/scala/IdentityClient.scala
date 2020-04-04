import little.cli.Cli.application
import little.cli.Implicits._
import org.apache.commons.cli.Option

object IdentityClient {

  val defaultPort = 5190

  val defaultPassword = "changeme"

  private[this] def ob(s: String): Option.Builder = Option.builder(s)

  private[this] val app = application(
    "java IdClient --server <serverhost> [--numport <port#>] <query>",
    ob("s").longOpt("server").desc("the identity server to connect to")
      .hasArg.argName("serverport").build,
    ob("n").longOpt("numport").desc("the port to connect on")
      .hasArg.argName("port#").`type`(Int.getClass).build,
    ob("c").longOpt("create").desc("create a new login; usable with --password")
      .hasArgs.numberOfArgs(2).argName("login name> <real name").build,
    ob("l").longOpt("lookup").desc("gives all information on the given user")
      .hasArg.argName("login name").build,
    ob("r").longOpt("reverse-lookup").desc("look up a user by UUID")
      .hasArg.argName("UUID").build,
    ob("m").longOpt("modify").desc("change your login name; password protected")
      .hasArgs.numberOfArgs(2).argName("old login name> <new login name").build,
    ob("d").longOpt("delete").desc("delete the account; password protected")
      .hasArg.argName("login name").build,
    ob("g").longOpt("get").desc("retrieve information from the server")
      .hasArg.argName("users|uids|all").build,
    ob("h").longOpt("help").desc("print a help message").build
  )

  def query(args: Array[String]): Unit = {
    val cmd = app.parse(args)
    if (cmd.hasOption('s')) {
      throw new IllegalArgumentException("must gives value for option 's'")
    }
    if (cmd.hasOptions("c", "l", "r", "m", "d", "g", "h").count(x => x) != 1) {
      throw new IllegalArgumentException("must give exactly 1 query option")
    }
    val server = cmd.getOptionValue('s')
    val port = scala.Option(cmd.getOptionValue('n'))
      .map(_.toInt).getOrElse(defaultPort)
    scala.Option(cmd.getOptionValue('p')) match {
      case Some(password) =>
        if (cmd.hasOption('c')) {

        } else if (cmd.hasOption('m')) {

        } else if (cmd.hasOption('d')) {

        } else {
          throw new IllegalArgumentException(
            "option is not compatible with the password option"
          )
        }
      case None =>
        if (cmd.hasOption('l')) {

        } else if (cmd.hasOption('r')) {

        } else if (cmd.hasOption('g')) {

        } else if (cmd.hasOption('h')) {

        } else {
          throw new IllegalArgumentException("option requires password option")
        }
    }
  }

  def main(args: Array[String]): Unit = query(args)
}
