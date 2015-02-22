package bittorrent.peer

import java.io.{File, FileOutputStream}
import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Actor, Props}
import bittorrent.data.Metainfo

import scala.collection.mutable
import scala.collection.mutable.{BitSet, ListBuffer}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class PeerManagerUpdate(var id:Int, val collected_pieces:Int, val total_pieces:Int)
class ActivePeersUpdate(var id:Int, var peers:Int)

// creates actors for each valid peer in the list from the tracker
class PeerCommunicationManager(metainfo: Metainfo,peer_id:Array[Byte],id:Int) extends Actor {
  var listeners:ListBuffer[ActorRef] = new ListBuffer[ActorRef]
  var nextId = 0
  val peerCommunicators = new ListBuffer[ActorRef]
  val num_pieces = metainfo.pieces.length
  val remaining_pieces:BitSet = BitSet((0 to num_pieces-1):_*)
  val pieces = new mutable.HashMap[Int,Array[Byte]]
  var saveFile:File = null
  val recent:ListBuffer[ActorRef] = new ListBuffer[ActorRef]
  var last:List[ActorRef] = List.empty[ActorRef]
  var all_peers:mutable.Set[(InetAddress,Short)] = mutable.Set.empty[(InetAddress,Short)]
  context.system.scheduler.schedule(Duration.create(3,TimeUnit.SECONDS),Duration.create(5,TimeUnit.SECONDS))(updateActive)
  //context.system.scheduler.schedule(Duration.create(20,TimeUnit.SECONDS),Duration.create(10,TimeUnit.SECONDS))(hammerPeers)

  def hammerPeers: Unit = {
    for ((ip,port) <- all_peers) {
      var worker: ActorRef = context.actorOf(Props(
        new PeerCommunicator(metainfo,peer_id,ip,port,nextId)),
        name="peerCommunicator"+id+nextId)
      worker ! "subscribe"
      peerCommunicators += worker
      worker ! remaining_pieces
      nextId += 1
    }
  }

  override def receive: Receive = {
    case ("complete",index:Int,piece:Array[Byte]) => {
      remaining_pieces -= index
      pieces.put(index,piece)
      println("Manager has "+pieces.keySet.size+" / "+metainfo.pieces.length+" pieces")
      notifyObservers
      peerCommunicators.foreach(x=>x ! remaining_pieces)
      teardown
      if (pieces.keySet.size == metainfo.pieces.length) teardown
      recent += sender
    }
    case "subscribe" => {
      listeners += sender
      notifyObservers
    }
    case peers:List[(InetAddress, Short)] => {
      for ((ip,port) <- peers) {
        var worker: ActorRef = context.actorOf(Props(
          new PeerCommunicator(metainfo,peer_id,ip,port,nextId)),
          name="peerCommunicator"+id+nextId)
        worker ! "subscribe"
        peerCommunicators += worker
        worker ! remaining_pieces
        nextId += 1
      }
      all_peers.++=(peers)
    }
    case (file:File) => {
      saveFile = file
    }
    case x => {
      println("PeerCommunicationManager received "+x)
    }
  }

  def notifyObservers: Unit = {
    val update = new PeerManagerUpdate(id,pieces.size,num_pieces)
    listeners.foreach(x=>x!update)
  }

  def updateActive: Unit = {
    val update = new ActivePeersUpdate(id, recent.size)
    listeners.foreach(x=>x!update)
    last = recent.toList
    recent.clear
  }

  private def teardown: Unit = {
    var complete: Array[Byte] = Array.empty[Byte]
    for (index <- 0 to num_pieces-1) {
      if (pieces.contains(index)) {
        complete = complete ++ pieces.get(index).get
      }
    }
    val out = new FileOutputStream(saveFile)
    out.write(complete)
    out.close()
    println("Wrote output to: "+saveFile)
  }
}