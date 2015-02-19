package bittorrent.peer

import java.net.InetAddress

import akka.actor.{Actor, Props}
import bittorrent.metainfo.Metainfo


// creates actors for each valid peer in the list from the tracker
class PeerCommunicationManager(metainfo: Metainfo,peer_id:String,peers:List[(InetAddress, Short)],id:Int) extends Actor {
  println("A PeerCommunicationManager was just created")
  var nextId = 0
  for ((ip,port) <- peers.take(1)) {
    var communicator = context.actorOf(Props(
      new PeerCommunicator(metainfo,peer_id,ip,port,nextId)),
      name="peerCommunicator"+id+nextId)
    nextId += 1
  }

  override def receive: Receive = {
    case x => {
      println("PeerCommunicationManager received "+x)
      println("which is weird because I didn't send it anything")
    }
  }
}