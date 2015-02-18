import java.io.File
import scala.io.Codec.ISO8859

import akka.actor.{ActorRef, Props, Actor}

import scala.io.Source

class FileManager extends Actor {
  val gui:ActorRef = null
  override def receive: Receive = {
    case file:File => {
      println("FileManager received a file")
      val src = Source.fromFile(file)(ISO8859)
      val metainfo = new Metainfo(src)
      var tracker: ActorRef = context.actorOf(Props(
        new TrackerCommunicator(metainfo)),
        name="trackercommunicator")
    }
    case x => {
      println("FileManager received an unknown message: "+x)
    }
  }
}
