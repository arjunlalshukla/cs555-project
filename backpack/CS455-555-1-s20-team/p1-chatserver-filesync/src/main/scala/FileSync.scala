import java.io.File

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
  val syncServer: String = args(0)
  val syncFolder = new File(args(1))

  if (syncFolder.exists && !syncFolder.isDirectory){
    FileSync.error("sync folder must be a directory")
  }else if (!syncFolder.exists){
    syncFolder.mkdir
  }

}

//noinspection SpellCheckingInspection
final class Server(args: Vector[String]) {
  if (args.length != 1) {
    FileSync.error(FileSync.usage)
  }

  val syncdir = new File(args(0))
  val fssdir = new File(".fss")
  val fssrc = new File(".fss/fssrc")

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

  private[this] val invalids = fssrckeys.keys.filter(Server.validKeys.contains)
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
}
object Server {
  val validKeys = Set("clientlist", "interval", "logfile", "timeout")
  // our group's valid ports
  val validPorts = Set(5190, 5191, 5192, 5193, 5194)
}