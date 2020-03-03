import java.io.{File, InputStream, ObjectInputStream, ObjectOutputStream}
import java.net.Socket
import java.net.ServerSocket

import scala.collection.mutable.ListBuffer
import scala.io.Source

object FileSync {

  val usage: String = """
    |usage:
    |scala FileSync -client <serverhost> <localfolder>
    |or
    |scala FileSync -server <folder>
    |""".stripMargin

  def main(args: Array[String]): Unit = {
    try {
      args.headOption match {
        case Some("-client") => new Client(args.tail.toSeq)
        case Some("-server") => new Server(args.tail.toSeq)
        case _ => println(usage)
      }
    } catch {
      case e: FileSyncException => println(e.getMessage)
    }
  }
}

final class Client(args: Seq[String]) {
  if (args.length != 2) {
    throw new FileSyncException(Seq(FileSync.usage))
  }
  val port: Int = Server.validPorts.head
  val syncServer: String = args.head
  val syncFolder: File = new File(args(1))

  if (syncFolder.exists && !syncFolder.isDirectory){
    throw new FileSyncException(Seq("sync folder must be a directory"))
  } else if (!syncFolder.exists){
    syncFolder.mkdir
  }

  val s = new Socket(syncServer, port)
  val in: InputStream = s.getInputStream
  val oin = new ObjectInputStream(in)
  val accepted: Boolean = oin.readBoolean
  if (accepted){
    println(s"connected to server $syncServer")
    //generate list of files in syncFolder
    val fileList = syncFolder.listFiles.map { file =>
      FileToSync(file.getName, file.lastModified, file.length)
    }.toSeq
    val oout = new ObjectOutputStream(s.getOutputStream)
    oout.writeObject(fileList)
    oout.flush()

  }else{
    println(s"could not connect to server $syncServer, unauthorized")
  }
}


//noinspection SpellCheckingInspection
final class Server(args: Seq[String]) {
  if (args.length != 1) {
    throw new FileSyncException(Seq(FileSync.usage))
  }

  val port: Int = Server.validPorts.head
  val syncdir: File = new File(args.head)
  val fssdir: File = new File(".fss")
  val fssrc: File = new File(".fss/fssrc")
  private[this] val errors = new ListBuffer[String]

  if (!syncdir.exists || !syncdir.isDirectory) {
    errors.append("folder given must exist and be a directory")
  } else if (syncdir.listFiles.exists(_.isDirectory)) {
    errors.append("folder given cannot contain subdirectories")
  }
  if (!fssdir.exists || !fssdir.isDirectory) {
    errors.append("The .fss file must exist and be a directory")
  } else if (!fssrc.exists || fssrc.isDirectory) {
    errors.append("The fssrc file must exists and not be a directory")
  }
  if (errors.nonEmpty) {
    throw new FileSyncException(errors.toSeq)
  }

  private[this] val rcsrc = Source.fromFile(fssrc.getAbsolutePath)
  val fssrclines: Seq[String] = rcsrc.getLines.toVector
    .map(_.trim).filter(_.length > 0) // for empty lines
  rcsrc.close

  if (fssrclines.map(_.filter(_ == '=').length).exists(_ != 1)) {
    throw new FileSyncException(
      Seq("fssrc must contain key value pairs with one equals per line"))
  }

  private[this] val fssrckeys = fssrclines.map { line =>
    val kv = line.split("=")
    kv(0).trim -> kv.lift(1).getOrElse("").trim
  }.toMap

  val clientlist: Set[String] = fssrckeys.getOrElse("clientlist", "") match {
    case "" => Set("localhost")
    case a => a.split(",").map(_.trim).toSet
  }

  val interval: Int = fssrckeys.getOrElse("interval", "") match {
    case "" => 60
    case a =>
      if (a.matches("\\d*")) {
        a.toInt
      } else {
        -1
      }
  }

  val logfile: File = fssrckeys.getOrElse("logfile", "") match {
    case "" => new File(fssdir.getAbsolutePath + "/log")
    case a => new File(a)
  }

  val timeout: Int = fssrckeys.getOrElse("timeout", "") match {
    case "" => 900
    case a =>
      if (a.matches("\\d*")) {
        a.toInt
      } else {
        -1
      }
  }

  private[this] val invalids = fssrckeys.keys.filter(!Server.validKeys.contains(_))
  if (interval == -1) {
    errors.append("interval must be a positive integer")
  }
  if (timeout == -1) {
    errors.append("timeout must be a positive integer")
  }
  if (logfile.exists && logfile.isDirectory) {
    errors.append("Log file cannot be a directory")
  }
  if (invalids.nonEmpty){
    errors.append(s"these keys given in the fssrc file are invalid: " +
      invalids.map("\"" + _ + "\"" ).mkString(", "))
  }
  if (errors.nonEmpty) {
    throw new FileSyncException(errors.toSeq)
  }

  println("Server configuration parameters:")
  println(s"clientlist: ${clientlist.mkString(", ")}")
  println(s"timeout: $timeout")
  println(s"logfile: $logfile")
  println(s"interval: $interval")

  private[this] val s = new ServerSocket(port)
  while (true){
    processClient(s.accept)
  }

  //serveClients
  def processClient(socket: Socket): Unit = {
    val oout = new ObjectOutputStream(socket.getOutputStream)
    val accepted = clientlist.contains(socket.getInetAddress.getHostName)
    oout.writeBoolean(accepted)
    oout.flush()
    if (accepted){
      println("Accepted connect from " + socket.getInetAddress.getHostAddress + ": " + socket.getPort)
      serve(socket)
    }else{
      println("Rejected connect from " + socket.getInetAddress.getHostAddress + ": " + socket.getPort)
    }
    socket.close()
  }

  def serve(socket: Socket): Unit = {
    val oin = new ObjectInputStream(socket.getInputStream)
    val clientFileList = oin.readObject.asInstanceOf[Seq[FileToSync]]
    println(clientFileList.mkString(", "))
  }
}
object Server {
  val validKeys = Set("clientlist", "interval", "logfile", "timeout")
  // our group's valid ports
  val validPorts = Seq(5190, 5191, 5192, 5193, 5194)
}

final class FileSyncException(val msgs: Seq[String])
  extends Exception(msgs.mkString("\n"))

case class FileToSync(fn: String, ts: Long, size: Long)