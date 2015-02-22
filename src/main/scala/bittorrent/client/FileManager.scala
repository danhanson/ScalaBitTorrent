package bittorrent.client

import java.io.File
import akka.actor.{Actor, ActorRef, Props}
import bittorrent.data.{Metainfo}
import bittorrent.peer.PeerManagerUpdate
import bittorrent.tracker.{HTTPTrackerCommunicator, TrackerStatusUpdate, UDPTrackerCommunicator}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Codec.ISO8859
import scala.io.Source

class FileManager extends Actor {
  @volatile var gui:ActorRef = null
  val saveFiles = new mutable.HashMap[Int, File]
  val trackerCommunicators = new mutable.HashMap[ActorRef,Int]
  val peerManagers = new mutable.HashMap[ActorRef,Int]
  val trackers = new mutable.HashMap[Int,List[ActorRef]]

  override def receive: Receive = {
    // sender does not work here because it is sent from an AWT component
    case (ref:ActorRef,id:Int, openFile:File, saveFile:File) => {
      gui = ref
      saveFiles.put(id, saveFile)
      val src = Source.fromFile(openFile)(ISO8859)
      val metainfo = new Metainfo(src)
      val torrent_trackers = new ListBuffer[ActorRef]
      var incr = 0
      val tracker: ActorRef = if (metainfo.announce.startsWith("http")) {
        context.actorOf(Props(new HTTPTrackerCommunicator(metainfo,metainfo.announce,id+incr)),name="trackercommunicator"+id+incr)
      } else {
        context.actorOf(Props(new UDPTrackerCommunicator(metainfo,metainfo.announce,id+incr)),name="trackercommunicator"+id+incr)
      }
      tracker ! "subscribe"
      trackerCommunicators.put(tracker,id)
      torrent_trackers += tracker
      incr += 1000
      for (my_announce <- metainfo.announceList.take(2)) {
        val tracker: ActorRef = if (my_announce.startsWith("http")) {
          context.actorOf(Props(new HTTPTrackerCommunicator(metainfo,my_announce,id+incr)),name="trackercommunicator"+id+incr)
        } else {
          context.actorOf(Props(new UDPTrackerCommunicator(metainfo,my_announce,id+incr)),name="trackercommunicator"+id+incr)
        }
        tracker ! "subscribe"
        trackerCommunicators.put(tracker,id)
        torrent_trackers += tracker
        incr += 1000
      }
      trackers.put(id,torrent_trackers.toList)
    }
    case update:TrackerStatusUpdate =>
      val real_id = trackerCommunicators.get(sender).get
      update.id = real_id
      gui ! update
    case update:PeerManagerUpdate =>
      val real_id = peerManagers.get(sender).get
      update.id = real_id
      gui ! update
    case peerManager:ActorRef => {
      val id = trackerCommunicators.get(sender).get
      peerManagers.put(peerManager,id)
      peerManager ! "subscribe"
      peerManager ! saveFiles.get(id).get
      trackers.get(id).get.foreach(x=>x!peerManager)
    }
    case x => {
      println("FileManager received an unknown message: "+x)
    }
  }
}
