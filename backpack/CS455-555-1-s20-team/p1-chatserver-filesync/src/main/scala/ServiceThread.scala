import java.io.{File, IOException}
import java.net.SocketException
import java.nio.file.{Files, NoSuchFileException}

import akka.actor.ActorRef

import scala.jdk.StreamConverters._

final class ServiceThread(
  private[this] val io: IOStream,
  private[this] val syncdir: File,
  private[this] val log: ActorRef
) extends Runnable {

  private[this] var _active = true

  def run(): Unit =
    try {
      initial()
      fileRequests()
      dirRequests()
    } catch {
      case _: IOException | _: SocketException => _active = false
    }

  def notification(): Unit =
    try {
      io.write(IntervalNotify())
    } catch {
      case _: IOException | _: SocketException => _active = false
    }

  def active: Boolean = _active

  def close(): Unit = io.sock.close()

  private[this] def initial(): Unit = {
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
    log ! WriteSeq(Seq(
      s"Client list: \n${clientFileList.keys.toSeq.sorted.mkString("\n")}\n",
      s"Server list: \n${serverFileList.keys.toSeq.sorted.mkString("\n")}\n",
      s"To delete: \n${delete.toSeq.sorted.mkString("\n")}\n",
      s"To update: \n${update.toSeq.sorted.mkString("\n")}\n"
    ))
    io.write(delete)
    io.write(update.filterNot(syncdir.toPath.resolve(_).toFile.isDirectory))
    io.write(update.filter(syncdir.toPath.resolve(_).toFile.isDirectory))
  }

  @scala.annotation.tailrec
  private[this] def fileRequests(): Unit =
    io.read[Option[String]].map(syncdir.toPath.resolve(_).toFile) match {
      case None =>
      case Some(file) =>
        // if the file does not exists, an empty file is written and will be
        // deleted on the next sync
        try {
          log ! Write(s"writing file to client: ${file.getAbsolutePath}")
          io.write(FileContents(file.lastModified, Files.readAllBytes(file.toPath)))
        } catch {
          case _: NoSuchFileException =>
            log ! Write(s"no such file found on server: ${file.getAbsolutePath}")
            io.write(FileContents(0, Array()))
        }
        fileRequests()
    }

  @scala.annotation.tailrec
  private[this] def dirRequests(): Unit =
    io.read[Option[String]].map(syncdir.toPath.resolve(_).toFile) match {
      case None =>
      case Some(file) =>
        try {
          log ! Write(s"giving dir timestamp to client: ${file.getAbsolutePath}")
          io.out.writeLong(file.lastModified)
          io.out.flush()
        } catch {
          case _: NoSuchFileException =>
            log ! Write(s"no such dir found on server: ${file.getAbsolutePath}")
            io.out.writeLong(0)
            io.out.flush()
        }
        dirRequests()
    }
}