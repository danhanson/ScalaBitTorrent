package bittorrent.peer

import java.io.{File, FileOutputStream}
import java.net.InetAddress

import akka.actor.{ActorRef, Actor, Props}
import bittorrent.metainfo.Metainfo

import scala.collection.mutable
import scala.collection.mutable.{BitSet, ListBuffer}

class PeerManagerUpdate(val id:Int, val collected_pieces:Int, val total_pieces:Int)

// creates actors for each valid peer in the list from the tracker
class PeerCommunicationManager(metainfo: Metainfo,peer_id:Array[Byte],peers:List[(InetAddress, Short)],id:Int) extends Actor {
  var listeners:ListBuffer[ActorRef] = new ListBuffer[ActorRef]
  var nextId = 0
  val peerCommunicators = new ListBuffer[ActorRef]
  val num_pieces = metainfo.pieces.length
  val remaining_pieces:BitSet = BitSet((0 to num_pieces-1):_*)
  val pieces = new mutable.HashMap[Int,Array[Byte]]
  var saveFile:File = null

  for ((ip,port) <- peers) {
    var worker: ActorRef = context.actorOf(Props(
      new PeerCommunicator(metainfo,peer_id,ip,port,nextId)),
      name="peerCommunicator"+id+nextId)
    worker ! "subscribe"
    peerCommunicators += worker
    nextId += 1
  }

  override def receive: Receive = {
    case ("complete",index:Int,piece:Array[Byte]) => {
      remaining_pieces -= index
      pieces.put(index,piece)
      println("Manager has "+pieces.keySet.size+" / "+metainfo.pieces.length+" pieces: "+pieces.mkString)
      notifyObservers
      sender ! remaining_pieces
      teardown
      if (pieces.keySet.size == metainfo.pieces.length) teardown
    }
    case "subscribe" => {
      listeners += sender
      notifyObservers
    }
    case (file:File) => {
      saveFile = file
    }
    case x => {
      println("PeerCommunicationManager received "+x)
    }
  }

  def notifyObservers: Unit = {
    println("PeerCommunicationManager sent it to "+listeners.size)
    val update = new PeerManagerUpdate(id,pieces.size,num_pieces)
    listeners.foreach(x=>x!update)
  }

  private def teardown: Unit = {
    var complete: Array[Byte] = Array.empty[Byte]
    for (index <- 0 to num_pieces-1) {
      if (pieces.contains(index)) {
        complete = complete ++ pieces.get(index).get
      }
    }
    println("Length of final result: "+complete.length)
    val out = new FileOutputStream(saveFile)
    out.write(complete)
    out.close()
    println("Wrote output to: "+saveFile)
  }
}