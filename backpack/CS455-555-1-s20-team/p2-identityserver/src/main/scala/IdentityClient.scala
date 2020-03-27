import little.cli.Cli.{application, option}
import little.cli.Implicits._
import org.apache.commons.cli.Option

class IdentityClient {

}
object IdentityClient {

  private[this] def ob(s: String) = Option.builder(s)

  private[this] val cmd = application(
    "java IdClient --server <serverhost> [--numport <port#>] <query>",
    ob("s").longOpt("server").desc("the identity server to connect to")
      .hasArg.argName("serverport>").required.build,
    ob("n").longOpt("numport").desc("the port to connect on")
      .hasArg.argName("port#").build
  )

  def main(args: Array[String]): Unit = {
  }
}
