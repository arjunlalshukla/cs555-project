import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

/**
 *  Driver class for starting the server or client
 */
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