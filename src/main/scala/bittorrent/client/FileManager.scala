package bittorrent.client

import java.io.File
import java.net.InetAddress
import akka.actor.{Actor, ActorRef, Props}
import bittorrent.data.{Metainfo}
import bittorrent.data.HTTPOnlyMetainfo
import bittorrent.peer.PeerManagerUpdate
import bittorrent.tracker.{TrackerStatusUpdate, TrackerCommunicator}
import scala.collection.mutable
import scala.io.Codec.ISO8859
import scala.io.Source

class FileManager extends Actor {
  var gui:ActorRef = null
  val saveFiles = new mutable.HashMap[Int, File]
  val trackerCommunicators = new mutable.HashMap[ActorRef,Int]
  val peerManagers = new mutable.HashMap[ActorRef,Int]

  override def receive: Receive = {
    // sender does not work here because it is sent from an AWT component
    case (ref:ActorRef,id:Int, openFile:File, saveFile:File) => {
      gui = ref
      saveFiles.put(id, saveFile)
      val src = Source.fromFile(openFile)(ISO8859)
      val metainfo = new HTTPOnlyMetainfo(src)
      val tracker: ActorRef = context.actorOf(Props(
        new TrackerCommunicator(metainfo, id)),
        name = "trackercommunicator" + id)
      tracker ! "subscribe"
      trackerCommunicators.put(tracker,id)
    }
    case update:TrackerStatusUpdate =>
      gui ! update
    case update:PeerManagerUpdate =>
      gui ! update
    case peerManager:ActorRef => {
      val id = trackerCommunicators.get(sender).get
      peerManagers.put(peerManager,id)
      peerManager ! "subscribe"
      peerManager ! saveFiles.get(id).get
    }
    case x => {
      println("FileManager received an unknown message: "+x)
    }
  }
}
