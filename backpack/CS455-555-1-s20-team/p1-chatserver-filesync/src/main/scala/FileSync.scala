import java.io.{File, IOException, ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}
import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.jdk.StreamConverters._

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

  val ios: IOStream = IOStream(new Socket(syncServer, port))
  val accepted: Boolean = ios.in.readBoolean
  private[this] val get_path =
    (s: String) => Paths.get(syncFolder.getAbsolutePath, s)
  if (accepted){
    println(s"connected to server $syncServer")
    try {
      while (true) {
        sync(ios)
      }
    } catch {
      case _: IOException => println("Connection with server lost")
    }
  } else{
    println(s"could not connect to server $syncServer, unauthorized")
  }

  def sync(io: IOStream): Unit = {
    io.read[IntervalNotify]
    //generate list of files in syncFolder
    val fileList = Files.walk(syncFolder.toPath).toScala(Set).map { path =>
      syncFolder.toPath.relativize(path).toString ->
        FileProperties(path.toFile.lastModified, path.toFile.length)
    }.filter(_._1 != "").toMap
    io.write(fileList)
    val delete = io.read[Set[String]]
    val fileUpdates = io.read[Set[String]]
    val dirUpdates = io.read[Set[String]]
    println(s"To delete:\n${delete.toSeq.sorted.mkString("\n")}\n")
    println(s"Files to update:\n${fileUpdates.toSeq.sorted.mkString("\n")}\n")
    println(s"Dirs to update:\n${dirUpdates.toSeq.sorted.mkString("\n")}\n")

    delete.foreach { path =>
      val p = get_path(path)
      // you cannot delete a directory that has files,
      // so we set recursively walk through in reverse order,
      // because walk() lists from the top down, depth first
      if (p.toFile.exists) {
        Files.walk(p).toScala(LazyList).reverse.foreach(_.toFile.delete())
      }
      println(s"deleted ${path.toString}")
    }

    fileUpdates.foreach { path =>
      io.write(Some(path))
      val contents = io.read[FileContents]
      val file = get_path(path).toFile
      file.getParentFile.mkdirs()
      Files.write(file.toPath, contents.bytes)
      file.setLastModified(contents.ts)
      println(s"Updated ${path.toString}")
    }
    io.write(None)

    dirUpdates.foreach { path =>
      io.write(Some(path))
      val ts = io.in.readLong
      val file = get_path(path).toFile
      file.getParentFile.mkdirs()
      file.mkdir()
      file.setLastModified(ts)
      println(s"updated ${path.toString}")
    }
    io.write(None)
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

  if (logfile.exists) {
    logfile.delete
  }

  //collect information that can be used for logfile
  private[this] val system = ActorSystem("LogFileSystem")
  private[this] val log = system.actorOf(Props(new IOActor(logfile.toPath)), name="serverLog")

  private[this] val logMessages = new ListBuffer[String]
  logMessages.append("Server configuration parameters:")
  logMessages.append(s"clientlist: ${clientlist.mkString(", ")}")
  logMessages.append(s"timeout: $timeout")
  logMessages.append(s"logfile: $logfile")
  logMessages.append(s"interval: $interval")
  println(logMessages.toSeq.mkString("\n"))
  log ! Write(logMessages.toSeq.mkString("\n"))


  private[this] val s = new ServerSocket(port)
  while (true){
    processClient(IOStream(s.accept))
  }

  //serveClients
  def processClient(io: IOStream): Unit = {
    val accepted = clientlist.contains(io.sock.getInetAddress.getHostName) ||
      clientlist.contains(io.sock.getInetAddress.getHostAddress)
    io.out.writeBoolean(accepted)
    io.out.flush()
    if (accepted){
      log ! Write("Accepted connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
      val thread = new ServiceThread(io, syncdir, log)
      while(true) {
        thread.run()
        Thread.sleep(60000)
      }
    }else{
      println("Rejected connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
      log ! Write("Rejected connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
    }
    io.sock.close()
  }
}
object Server {
  val validKeys = Set("clientlist", "interval", "logfile", "timeout")
  // our group's valid ports
  val validPorts = Seq(5190, 5191, 5192, 5193, 5194)
}

final class ServiceThread(
  private[this] val io: IOStream,
  private[this] val syncdir: File,
  private[this] val log: ActorRef
) extends Runnable {
  def run(): Unit = {
    initial()
    fileRequests()
    dirRequests()
  }

  private[this] def initial(): Unit = {
    io.write(IntervalNotify())
    val clientFileList = io.read[Map[String, FileProperties]]
    val serverFileList = Files.walk(syncdir.toPath).toScala(Set).map { path =>
      syncdir.toPath.relativize(path).toString ->
        FileProperties(path.toFile.lastModified, path.toFile.length)
    }.filter(_._1 != "").toMap
    // ^ we need this filter because walk() automatically includes syncdir as
    // and empty string
    val delete: Set[String] = clientFileList.keySet diff serverFileList.keySet
    val update: Set[String] = serverFileList.keySet diff clientFileList.keySet union
      (serverFileList.keySet intersect clientFileList.keySet)
        .filter(s => serverFileList(s) != clientFileList(s))
    log ! Write(s"Client list: \n${clientFileList.keys.toSeq.sorted.mkString("\n")}\n")
    log ! Write(s"Server list: \n${serverFileList.keys.toSeq.sorted.mkString("\n")}\n")
    log ! Write(s"To delete: \n${delete.toSeq.sorted.mkString("\n")}\n")
    log ! Write(s"To update: \n${update.toSeq.sorted.mkString("\n")}\n")
    io.write(delete)
    io.write(update.filterNot(syncdir.toPath.resolve(_).toFile.isDirectory))
    io.write(update.filter(syncdir.toPath.resolve(_).toFile.isDirectory))
  }

  @scala.annotation.tailrec
  private[this] def fileRequests(): Unit =
    io.read[Option[String]].map(syncdir.toPath.resolve(_).toFile) match {
      case None =>
      case Some(file) =>
        log ! Write(s"writing file to client: ${file.getAbsolutePath}")
        io.write(FileContents(file.lastModified, Files.readAllBytes(file.toPath)))
        fileRequests()
    }

  @scala.annotation.tailrec
  private[this] def dirRequests(): Unit =
    io.read[Option[String]].map(syncdir.toPath.resolve(_).toFile) match {
      case None =>
      case Some(file) =>
        log ! Write(s"giving dir timestamp to client: ${file.getAbsolutePath}")
        io.out.writeLong(file.lastModified)
        io.out.flush()
        dirRequests()
    }
}

final class FileSyncException(val msgs: Seq[String])
  extends Exception(msgs.mkString("\n"))

case class FileToSync(fn: String, ts: Long, size: Long)
case class FileProperties(ts: Long, size: Long)
case class FileContents(ts: Long, bytes: Array[Byte])

case class IntervalNotify()

case class IOStream(sock: Socket) {
  val out = new ObjectOutputStream(sock.getOutputStream)
  val in = new ObjectInputStream(sock.getInputStream)
  def read[T]: T = in.readObject.asInstanceOf[T]
  def write(o: AnyRef): Unit = {
    out.writeObject(o)
    out.flush()
  }
}