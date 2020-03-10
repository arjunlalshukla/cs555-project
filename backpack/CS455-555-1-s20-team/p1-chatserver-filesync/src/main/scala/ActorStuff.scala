import java.io.{FileWriter, PrintWriter}
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, TimeZone}

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.Actor
//import os._;

abstract sealed class IOCmd
case class Write(s: String)
case class WriteSeq(s: Seq[String])

final class IOActor(outfile: Path) extends Actor {
  def receive: Receive = {
    case Write(s) => write(Seq(s))
    case WriteSeq(s) => write(s)
  }

  private[this] def write(s: Seq[String]) = {
    val timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat: SimpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy", Locale.US)
    val dateTime = Calendar.getInstance(timeZone)
    dateFormat.setTimeZone(timeZone)
    val currentHour = dateFormat.format(dateTime.getTime)
    val fileWriter = new FileWriter(outfile.toFile,true)
    val printWriter = new PrintWriter(fileWriter)
    printWriter.println(s"$currentHour ${s.mkString("\n")}")
    printWriter.close()
  }
}