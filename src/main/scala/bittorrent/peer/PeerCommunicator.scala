package bittorrent.peer

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.Actor
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import bittorrent.metainfo.Metainfo
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}


class PeerCommunicator(metainfo:Metainfo,peer_id:String,address:InetAddress,port:Short,id:Int) extends Actor {
  import context.system
  import bittorrent.peer.Choke._
  import bittorrent.peer.Interest._
  println("A PeerCommunicator was just created")
  var am_choking:Choke = Choke.Choking
  var peer_choking:Choke = Choking
  var am_interested:Interest = NotInterested
  var peer_interested:Interest = NotInterested
  var remote = new InetSocketAddress(address,port)

  val manager = IO(Tcp)
  IO(Tcp) ! Connect(remote)

  override def receive: Receive = {
    case CommandFailed(x: Connect) => {
      println("Got this one3345")
      println(x)
    }
    case x @ Connected(remote,local) => {
      println("WTF is this syntax")
      println(x)
      println(x.getClass)
      println("Message from: "+sender)
      val connection = sender()
      connection ! Register(self)
      connection ! Tcp.Write(ByteString(handshake))
      context become {
        case Received(msg:ByteString) => {
          println("I GOT A MESSAGE FROM A PEER")
          println(msg)
        }
        case PeerClosed => {
          println("Got a PeerClosed, not sure what to do though")
          context stop self
        }
        case x => {
          println("Im not really sure what this could be")
          println(x)
          println(x.getClass)

        }
      }
    }
    case x => {
      println("PeerCommunicator recieved "+x)
    }
  }

  def handshake : Array[Byte] = {
    val pstr = "BitTorrent protocol".getBytes
    val handshakeBytes: ChannelBuffer = ChannelBuffers.buffer(68)
    handshakeBytes.writeByte(19)
    for(i <- 0 until 19)
      handshakeBytes.writeByte(pstr(i))
    for(i <- 0 until 8)
      handshakeBytes.writeByte(0)
    for(i <- 0 until 20)
      handshakeBytes.writeByte(metainfo.infohash(i))
    for(i <- 0 until 20)
      handshakeBytes.writeByte(peer_id.getBytes()(i))
    println("The handshake is: "+handshakeBytes.array.mkString(", "))
    handshakeBytes.array
  }
}

object Choke extends Enumeration {
  type Choke = Value
  val Choking, NotChoking = Value
}

object Interest extends Enumeration {
  type Interest = Value
  val Interested, NotInterested = Value
}