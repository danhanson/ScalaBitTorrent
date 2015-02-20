package bittorrent.peer

import java.net.InetAddress

import akka.actor.{ActorRef, Actor, Props}
import bittorrent.metainfo.Metainfo

import scala.collection.mutable.ListBuffer


// creates actors for each valid peer in the list from the tracker
class PeerCommunicationManager(metainfo: Metainfo,peer_id:Array[Byte],peers:List[(InetAddress, Short)],id:Int) extends Actor {
  var nextId = 0
  val peerCommunicators = new ListBuffer[ActorRef]

  // the `take(1)` on the following line should be removed once we get everything working
  for ((ip,port) <- peers.take(2)) {
    var worker: ActorRef = context.actorOf(Props(
      new PeerCommunicator(metainfo,peer_id,ip,port,nextId)),
      name="peerCommunicator"+id+nextId)
    worker ! "subscribe"
    peerCommunicators += worker
    nextId += 1
  }

  override def receive: Receive = {
    case ("Complete",index:Int,piece:Array[Byte]) => {
      println("Manager received a piece from a worker")
    }
    case x => {
      println("PeerCommunicationManager received "+x)
    }
  }
}