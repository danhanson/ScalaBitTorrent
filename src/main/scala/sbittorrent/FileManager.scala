package sbittorrent
import java.io.File
import scala.io.Codec.ISO8859

import akka.actor.{ActorRef, Props, Actor}
import scala.collection.mutable.ListBuffer

import scala.io.Source

class FileManager extends Actor {
  var trackerCounter = 0
  val gui:ActorRef = null
  val trackerCommunicators = new ListBuffer[ActorRef]
  override def receive: Receive = {
    case file:File => {
      val src = Source.fromFile(file)(ISO8859)
      val metainfo = new HTTPOnlyMetainfo(src)
      var tracker: ActorRef = context.actorOf(Props(
        new TrackerCommunicator(metainfo)),
        name="trackercommunicator"+trackerCounter)
      trackerCounter += 1
      trackerCommunicators += tracker
    }
    case x => {
      println("FileManager received an unknown message: "+x)
    }
  }
}
