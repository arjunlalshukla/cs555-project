import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.Actor;
import os._;

abstract sealed class IOCmd
case class Write(s: String)
case class WriteSeq(s: Seq[String])

final class IOActor(outfile: Path) extends Actor {
  def receive: Receive = {
    case Write(s) => write(Seq(s))
    case WriteSeq(s) => write(s)
  }

  private[this] def write(s: Seq[String]) = {

  }
}