import java.io.{File, ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}
import java.nio.file.{Files, Paths}

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

  val io: IOStream = IOStream(new Socket(syncServer, port))

  println("connected and built IO")
  val accepted: Boolean = io.in.readBoolean
  private[this] val get_path = (s: String) => Paths.get(syncFolder.getAbsolutePath, s)
  if (accepted){
    println(s"connected to server $syncServer")
    //generate list of files in syncFolder
    val fileList = syncFolder.listFiles.map { file =>
      file.getName -> FileProperties(file.lastModified, file.length)
    }.toMap
    io.write(fileList)
    val delete = io.read[Set[String]]
    val update = io.read[Set[String]]
    println(s"To delete:\n ${delete.mkString("\n")}")
    println(s"To update:\n ${update.mkString("\n")}")
    delete.foreach { path =>
      get_path(path).toFile.delete()
      println(s"deleted ${path.toString}")
    }
    update.foreach { path =>
      io.write(Some(path))
      val contents = io.read[FileContents]
      val file = get_path(path).toFile
      Files.write(file.toPath, contents.bytes)
      file.setLastModified(contents.ts)
      println(s"Updated ${path.toString}")
    }
    io.write(None)

  } else{
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
    processClient(IOStream(s.accept))
  }

  //serveClients
  def processClient(io: IOStream): Unit = {
    val accepted = clientlist.contains(io.sock.getInetAddress.getHostName)
    io.out.writeBoolean(accepted)
    io.out.flush()
    if (accepted){
      println("Accepted connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
      serve(io)
    }else{
      println("Rejected connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
    }
    io.sock.close()
  }

  def serve(io: IOStream): Unit = {
    val clientFileList = io.read[Map[String, FileProperties]]
    val serverFileList = syncdir.listFiles.map { file =>
      file.getName -> FileProperties(file.lastModified, file.length)
    }.toMap
    val delete: Set[String] = clientFileList.keySet diff serverFileList.keySet
    val update: Set[String] = serverFileList.keySet diff clientFileList.keySet union
      (serverFileList.keySet intersect clientFileList.keySet)
        .filter(s => serverFileList(s) != clientFileList(s))
    println(s"Client list: ${clientFileList.keys.mkString(", ")}")
    println(s"Server list: ${serverFileList.keys.mkString(", ")}")
    println(s"To delete: ${delete.mkString(", ")}")
    println(s"To update: ${update.mkString(", ")}")
    io.write(delete)
    io.write(update)
    processRequests(io)
  }

  @scala.annotation.tailrec
  def processRequests(io: IOStream): Unit =
    io.read[Option[String]].map(syncdir.toPath.resolve(_).toFile) match {
      case None =>
      case Some(file) =>
        println(s"writing to client: ${file.getAbsolutePath}")
        io.write(FileContents(file.lastModified, Files.readAllBytes(file.toPath)))
        processRequests(io)
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
case class FileProperties(ts: Long, size: Long)
case class FileContents(ts: Long, bytes: Array[Byte])

case class IOStream(sock: Socket) {
  val out = new ObjectOutputStream(sock.getOutputStream)
  val in = new ObjectInputStream(sock.getInputStream)
  def read[T]: T = in.readObject.asInstanceOf[T]
  def write(o: AnyRef): Unit = {
    out.writeObject(o)
    out.flush()
  }
}