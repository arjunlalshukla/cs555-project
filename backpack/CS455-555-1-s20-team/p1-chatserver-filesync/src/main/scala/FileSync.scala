import java.io.{File, InputStream, ObjectInputStream, ObjectOutputStream}
import java.net.Socket
import java.net.ServerSocket

import scala.io.Source

object FileSync {

  val usage: String = """
    |usage:
    |scala FileSync -client <serverhost> <localfolder>
    |or
    |scala FileSync -server <folder>
    |""".stripMargin

  def error(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }

  def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some("-client") => new Client(args.tail.toVector)
      case Some("-server") => new Server(args.tail.toVector)
      case _ => println(usage)
    }
  }
}

final class Client(args: Vector[String]) {
  if (args.length != 2) {
    FileSync.error(FileSync.usage)
  }
  val port: Int = Server.validPorts(0)
  val syncServer: String = args(0)
  val syncFolder: File = new File(args(1))

  if (syncFolder.exists && !syncFolder.isDirectory){
    FileSync.error("sync folder must be a directory")
  }else if (!syncFolder.exists){
    syncFolder.mkdir
  }

  val s = new Socket(syncServer, port)
  val in: InputStream = s.getInputStream
  val oin = new ObjectInputStream(in)


}

//noinspection SpellCheckingInspection
final class Server(args: Vector[String]) {
  if (args.length != 1) {
    FileSync.error(FileSync.usage)
  }

  val port: Int = Server.validPorts(0)
  val syncdir: File = new File(args(0))
  val fssdir: File = new File(".fss")
  val fssrc: File = new File(".fss/fssrc")

  if (!syncdir.exists || !syncdir.isDirectory) {
    FileSync.error("folder given must exist and be a directory")
  } else if (syncdir.listFiles.exists(_.isDirectory)) {
    FileSync.error("folder given cannot contain subdirectories")
  } else if (!fssdir.exists || !fssdir.isDirectory) {
    FileSync.error("The .fss file must exist and be a directory")
  } else if (!fssrc.exists || fssrc.isDirectory) {
    FileSync.error("The fssrc file must exists and not be a directory")
  }

  private[this] val rcsrc = Source.fromFile(fssrc.getAbsolutePath)
  val fssrclines: Seq[String] = rcsrc.getLines.toVector
    .map(_.trim).filter(_.length > 0) // for empty lines
  rcsrc.close

  if (fssrclines.map(_.filter(_ == '=').length).exists(_ != 1)) {
    FileSync.error("fssrc must contain key value pairs with one equals per line")
  }

  private[this] val fssrckeys = fssrclines.map { line =>
    val kv = line.split("=")
    kv(0).trim -> kv.lift(1).getOrElse("").trim
  }.toMap

  val clientlist: Seq[String] = fssrckeys.get("clientlist") match {
    case None | Some("") => Vector("localhost")
    case Some(a) => a.split(",").map(_.trim).toVector
  }

  val interval: Int = fssrckeys.get("interval") match {
    case None | Some("") => 60
    case Some(a) =>
      if (a.matches("\\d*")) {
        a.toInt
      } else {
        -1
      }
  }

  val logfile: File = fssrckeys.get("logfile") match {
    case None | Some("") => new File(fssdir.getAbsolutePath + "/log")
    case Some(a) => new File(a)
  }

  val timeout: Int = fssrckeys.get("timeout") match {
    case None | Some("") => 900
    case Some(a) =>
      if (a.matches("\\d*")) {
        a.toInt
      } else {
        -1
      }
  }

  private[this] val invalids = fssrckeys.keys.filter(!Server.validKeys.contains(_))
  if (interval == -1) {
    FileSync.error("interval must be a positive integer")
  } else if (timeout == -1) {
    FileSync.error("timeout must be a positive integer")
  } else if (logfile.exists && logfile.isDirectory) {
    FileSync.error("Log file cannot be a directory")
  } else if (invalids.nonEmpty){
    FileSync.error(s"these keys given in the fssrc file are invalid: " +
      invalids.map("\"" + _ + "\"" ).mkString(", "))
  }

  println("Server configuration parameters:")
  println(s"clientlist: ${clientlist.mkString(", ")}")
  println(s"timeout: $timeout")
  println(s"logfile: $logfile")
  println(s"interval: $interval")

  private[this] val s = new ServerSocket(port)
  while (true){
      serveClient(s.accept)
  }

  //serveClients
  def serveClient(socket: Socket): Unit = {

    val out = socket.getOutputStream
    val in = socket.getInputStream

    // Note that client gets a temporary/transient port on it's side
    // to talk to the server on its well known port
    System.out.println(
      "Received connect from " + socket.getInetAddress.getHostAddress + ": " + socket.getPort)

    val oout = new ObjectOutputStream(out)
    oout.writeObject(new java.util.Date)
    oout.flush()

    socket.close()
  }
}

object Server {
  val validKeys = Set("clientlist", "interval", "logfile", "timeout")
  // our group's valid ports
  val validPorts = Vector(5190, 5191, 5192, 5193, 5194)
}