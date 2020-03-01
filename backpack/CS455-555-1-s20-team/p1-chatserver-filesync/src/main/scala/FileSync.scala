import java.io.File

import scala.io.Source

object FileSync extends App {
  val usage = """
    |usage:
    |scala FileSync -client <serverhost> <localfolder>
    |or
    |scala FileSync -server <folder>
    |""".stripMargin
  args.headOption match {
    case Some("-client") => new Client(args.tail.toVector)
    case Some("-server") => new Server(args.tail.toVector)
    case _ => println(usage)
  }

  def error(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }
}

final class Client(args: Vector[String]) {

}

final class Server(args: Vector[String]) {
  val syncdir = new File(args(0))
  val fssdir = new File(".fss")
  val fssrc = new File(".fss/fssrc")

  if (args.length != 1) {
    FileSync.error(FileSync.usage)
  } else if (syncdir.listFiles.exists(_.isDirectory)) {
    FileSync.error("folder given cannot contain subdirectories")
  } else if (!fssdir.exists || !fssdir.isDirectory) {
    FileSync.error("The .fss file must exist and be a directory")
  } else if (!fssrc.exists || fssrc.isDirectory) {
    FileSync.error("The fssrc file must exists and not be a directory")
  }

  val fssrclines = Source.fromFile(fssrc.getAbsolutePath).getLines.toVector
    .map(_.trim).filter(_.length > 0) // for empty lines

  if (fssrclines.map(_.filter(_ == '=').length).exists(_ != 1)) {
    FileSync.error("fssrc must contain key value pairs with one equals per line")
  }

  val fssrckeys = fssrclines.map { line =>
    val kv = line.split("=")
    kv(0).trim -> kv(0).trim
  }.toMap
}