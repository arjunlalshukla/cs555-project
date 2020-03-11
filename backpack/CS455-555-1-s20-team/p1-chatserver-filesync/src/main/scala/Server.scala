import java.io.File
import java.net.ServerSocket
import java.util.{Timer, TimerTask}
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{ActorSystem, Props}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

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
  private[this] val log =
    system.actorOf(Props(new IOActor(logfile.toPath, true)), name="serverLog")

  log ! WriteSeq(Seq(
    "Server configuration parameters:",
    s"clientlist: ${clientlist.mkString(", ")}",
    s"timeout: $timeout",
    s"logfile: $logfile",
    s"interval: $interval",
  ))

  private[this] val threadList = new mutable.HashSet[ServiceThread]

  sys.addShutdownHook {
    threadList.foreach(_.close())
    println("Shutting down the server")
  }

  TimeoutHandler.restart()

  private[this] val s = new ServerSocket(port)
  while (true){
    processClient(IOStream(s.accept))
  }

  //serveClients
  def processClient(io: IOStream): Unit = threadList.synchronized {
    val accepted = threadList.size < 4 && (
      clientlist.contains(io.sock.getInetAddress.getHostName) ||
      clientlist.contains(io.sock.getInetAddress.getHostAddress)
    )
    io.out.writeBoolean(accepted)
    io.out.flush()
    if (accepted) {
      log ! Write("Accepted connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
      threadList add new ServiceThread(io, syncdir, log)
      TimeoutHandler.cancel()
      if (threadList.size == 1) {
        IntervalHandler.restart()
      }
    } else {
      log ! Write("Rejected connect from " + io.sock.getInetAddress.getHostAddress + ": " + io.sock.getPort)
      io.sock.close()
    }
  }

  private[this] object IntervalHandler {

    private[this] var timer = new Timer()

    private[this] final class IntervalTimer extends TimerTask {
      def run(): Unit = threadList.synchronized {
        threadList.filterInPlace(_.active)
        threadList.foreach(_.notification())
        threadList.filterInPlace(_.active)
        val pool = Executors.newFixedThreadPool(4)
        threadList.foreach(pool.execute)
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.MINUTES)
        threadList.filterInPlace(_.active)
        if (threadList.isEmpty) {
          this.cancel()
          TimeoutHandler.restart()
        }
      }
    }

    def restart(): Unit = {
      timer = new Timer()
      timer.schedule(new IntervalTimer, 0, interval*1000)
    }
  }

  private[this] object TimeoutHandler {

    private[this] var timer = new Timer()

    private[this] final class TimeoutTimer extends TimerTask {
      def run(): Unit = {
        log ! Write(s"Server timed out after $timeout seconds")
        System.exit(0)
      }
    }

    def restart(): Unit = {
      timer = new Timer()
      timer.schedule(new TimeoutTimer, timeout*1000)
    }

    def cancel(): Unit = timer.cancel()
  }
}
object Server {
  val validKeys = Set("clientlist", "interval", "logfile", "timeout")
  // our group's valid ports
  val validPorts = Seq(5190, 5191, 5192, 5193, 5194)
}