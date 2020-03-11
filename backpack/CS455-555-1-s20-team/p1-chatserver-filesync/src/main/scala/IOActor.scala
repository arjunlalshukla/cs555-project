import java.io.{FileWriter, PrintWriter}
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.Actor

abstract sealed class IOCmd
case class Write(s: String)
case class WriteSeq(s: Seq[String])

final class IOActor(outfile: Path, toConsole: Boolean) extends Actor {
  def receive: Receive = {
    case Write(s) => write(Seq(s))
    case WriteSeq(s) => write(s)
  }

  private[this] def write(s: Seq[String]): Unit = {
    val timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat: SimpleDateFormat =
      new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy", Locale.US)
    val dateTime = Calendar.getInstance(timeZone)
    dateFormat.setTimeZone(timeZone)
    val currentHour = dateFormat.format(dateTime.getTime)
    val fileWriter = new FileWriter(outfile.toFile,true)
    val printWriter = new PrintWriter(fileWriter)
    if (toConsole) {
      println(s"$currentHour ${s.mkString("\n")}")
    }
    printWriter.println(s"$currentHour ${s.mkString("\n")}")
    printWriter.close()
  }
}