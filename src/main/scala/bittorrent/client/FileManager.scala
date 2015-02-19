package bittorrent.client

import java.io.File
import java.net.InetAddress

import akka.actor.{Actor, ActorRef, Props}
import bittorrent.metainfo.{Metainfo, HTTPOnlyMetainfo}
import bittorrent.tracker.TrackerCommunicator

import scala.collection.mutable.ListBuffer
import scala.io.Codec.ISO8859
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
        new TrackerCommunicator(metainfo,trackerCounter)),
        name="trackercommunicator"+trackerCounter)
      //tracker ! "subscribe"
      trackerCounter += 1
      trackerCommunicators += tracker
    }
    case ("start",metainfo:Metainfo,peers:List[(InetAddress, Int)]) => {
      println("Time to start up")
    }
    case x => {
      println("FileManager received an unknown message: "+x)
    }
  }
}
