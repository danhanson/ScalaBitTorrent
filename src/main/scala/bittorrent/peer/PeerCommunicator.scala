package bittorrent.peer

import java.net.{InetAddress, InetSocketAddress}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Kill, ActorRef, Actor}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import bittorrent.data.Metainfo
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}

import scala.collection.mutable.{ListBuffer, BitSet}
import scala.concurrent.duration._
import scala.util.Random

class PeerCommunicator(metainfo:Metainfo,my_peer_id:Array[Byte],address:InetAddress,port:Short,id:Int) extends Actor {
  import context.system
  import bittorrent.peer.Choke._
  import bittorrent.peer.Interest._
  val protocol:String = "BitTorrent protocol"
  val block_size:Int = 16384  // 2^14
  var am_choking:Choke = Choke.Choking
  var peer_choking:Choke = Choking
  var am_interested:Interest = NotInterested
  var peer_interested:Interest = NotInterested
  var remote = new InetSocketAddress(address,port&0xFFFF)
  var his_peer_id:Array[Byte] = null
  var completed_handshake:Boolean = false
  var remaining_pieces:BitSet = BitSet((0 to metainfo.total_pieces-1):_*)
  var peer_pieces: BitSet = BitSet()
  //val peer_pieces:BitSet = BitSet((0 to metainfo.pieces.length-1):_*)   // this is just for testing purposes
  val num_total_pieces:Int = metainfo.total_pieces
  var connection:ActorRef = null
  var watchers:ListBuffer[ActorRef] = ListBuffer.empty[ActorRef]
  var continue_parsing:ByteString=>(Int,Int,Array[Byte]) = null
  var acted_recently = false
  var dht_port:Short = 0

  val manager = IO(Tcp)
  IO(Tcp) ! Connect(remote)
  context.system.scheduler.schedule(1 second,8 second)(takeInitiative)

  override def receive: Receive = {
    case x @ Connected(remote,local) => {
      connection = sender
      connection ! Register(self)
      connection ! Tcp.Write(ByteString(handshake))
      acted_recently = true
      context become {
        case Received(msg:ByteString) => {
          if (!completed_handshake) {
            parseHandshake(msg)
            //sendBitfield
            sendInterest
            //sendUnchoke
          } else if(continue_parsing != null) {
            continue_parsing(msg) match {
              case (index:Int,-1,piece:Array[Byte]) => {
                watchers.foreach(w=>w!("complete",index,piece))
                watchers.foreach(x=>x!"block")
                remaining_pieces -= index
                continue_parsing = null
                requestPiece
              }
              case (piece_num:Int,offset:Int,null) => {
                watchers.foreach(x=>x!"block")
                requestPiece(piece_num,offset)
              }
              case null => { }
            }
          } else {
            parseMessage(msg)
          }
        }
        case PeerClosed => {
          //println(PeerClosed)
          context stop self
        }
        case (index:Int,update:BitSet) => {
          remaining_pieces = update
          if (index > 0) sendHave(index)
        }
        case "subscribe" =>
          watchers += sender
        case (index:Int,begin:Int,block:Array[Byte]) =>
          sendBlock(index,begin,block)
        case x => {
          println("Why the fuck am I getting this message: "+x)
        }
      }
    }
    case (index:Int,begin:Int,block:Array[Byte]) =>
      sendBlock(index,begin,block)
    case "subscribe" =>
      watchers += sender
    case (index:Int,update:BitSet) => {
      remaining_pieces = update
      if (index > 0) sendHave(index)
    }
    case CommandFailed => {
      connection ! Tcp.Write(ByteString(handshake))     // probably not what you're supposed to do
    }
    case x =>
      println("PeerCommunicator recieved "+x)
  }


  def sendBlock(index:Int,begin:Int,block:Array[Byte]) {
    val blockBytes:ChannelBuffer = ChannelBuffers.buffer(4+9+block.size)
    blockBytes.writeInt(9+block.size)
    blockBytes.writeByte(7)
    blockBytes.writeInt(index)
    blockBytes.writeInt(begin)
    blockBytes.writeBytes(block)
    connection ! Tcp.Write(ByteString(blockBytes.array))
  }

  def sendHave(index:Int): Unit = {
    val haveBytes:ChannelBuffer = ChannelBuffers.buffer(4+5)
    haveBytes.writeInt(5)
    haveBytes.writeByte(4)
    haveBytes.writeInt(index)
    if (connection != null) connection ! Tcp.Write(ByteString(haveBytes.array))
  }

  def sendBitfield: Unit = {
    connection ! Tcp.Write(ByteString(bitfield))
  }

  def requestPiece: Unit = {
    if (remaining_pieces.isEmpty) {
      self ! Kill
      return
    }
    if (peer_pieces.isEmpty) peer_pieces = BitSet((0 to metainfo.total_pieces-1):_*)
    val choices: Array[Int] = (peer_pieces & remaining_pieces).toArray
    val piece_num:Int = choices.drop(Random.nextInt(choices.size)).head
      val offset = 0
      connection ! Tcp.Write(ByteString(requestBlock(piece_num,offset)))
      acted_recently = true
      //println("Just requested piece #"+piece_num+" with offset "+offset)

  }

  def requestPiece(piece_num:Int,offset:Int): Unit = {
    connection ! Tcp.Write(ByteString(requestBlock(piece_num,offset)))
    acted_recently = true
    //println("Just requested piece #"+piece_num+" with offset "+offset)
  }

  def requestBlock(piece_num:Int,offset:Int): Array[Byte] = {
    val requestBytes:ChannelBuffer = ChannelBuffers.buffer(17)
    requestBytes.writeInt(13)         // 4 bytes
    requestBytes.writeByte(6)         // 1 byte
    requestBytes.writeInt(piece_num)  // 4 bytes
    requestBytes.writeInt(offset)     // 4 bytes
    if (piece_num*metainfo.pieceLength+offset + block_size > metainfo.fileLength) {
      requestBytes.writeInt(metainfo.fileLength - offset - piece_num*metainfo.pieceLength)
      //println("Length of request was: "+(metainfo.fileLength - offset - piece_num*metainfo.pieceLength))
    } else if (offset + block_size > metainfo.pieceLength) {
      requestBytes.writeInt(metainfo.pieceLength - offset)
      //println("Length of request was: "+(metainfo.pieceLength-offset))
    } else {
      requestBytes.writeInt(block_size) // 4 bytes
      //println("Length of request was: "+block_size)
    }
    requestBytes.array
  }

  def sendUnchoke: Unit = {
    connection ! Tcp.Write(ByteString(unchoke))
    acted_recently = true
    peer_choking = NotChoking
  }

  def unchoke:Array[Byte] = {
    val unchokeBytes:ChannelBuffer = ChannelBuffers.buffer(5)
    unchokeBytes.writeInt(1)
    unchokeBytes.writeByte(1)
    unchokeBytes.array
  }

  def sendInterest: Unit = {
    connection ! Tcp.Write(ByteString(interested))
    acted_recently = true
    am_interested = Interested
  }

  def interested: Array[Byte] = {
    // 0-0-0-1-2
    val interestBytes: ChannelBuffer = ChannelBuffers.buffer(5)
    interestBytes.writeInt(1)
    interestBytes.writeByte(2)
    interestBytes.array
  }

  def bitfield:Array[Byte] = {
    val bitfieldBytes = ChannelBuffers.buffer(4+1+num_total_pieces/8)
    bitfieldBytes.writeInt(1+num_total_pieces/8)
    bitfieldBytes.writeByte(5)
    bitfieldBytes.writeBytes((0 to num_total_pieces/8-1).map(_=>0.toByte).toArray)
    bitfieldBytes.array
  }

  def handshake : Array[Byte] = {
    val pstr = protocol.getBytes
    val handshakeBytes: ChannelBuffer = ChannelBuffers.buffer(68)
    handshakeBytes.writeByte(19)
    for(i <- 0 until 19)
      handshakeBytes.writeByte(pstr(i))
    for(i <- 0 until 8)
      handshakeBytes.writeByte(0)
    for(i <- 0 until 20)
      handshakeBytes.writeByte(metainfo.infohash(i))
    for(i <- 0 until 20)
      handshakeBytes.writeByte(my_peer_id(i))
    handshakeBytes.array
  }

  def parseHandshake(bytes:ByteString): Unit = {
    val pstrlen:Byte = bytes.head
    val pstr:String = bytes.tail.take(pstrlen).toString
    val reserved:ByteString = bytes.drop(pstrlen+1).take(8)
    val info_hash:ByteString = bytes.drop(pstrlen+9).take(20)
    val peer_id:ByteString = bytes.drop(pstrlen+29).take(20)
    his_peer_id = peer_id.toArray
    completed_handshake = true
  }

  def parseMessage(bytes:ByteString): Unit = {
    if (bytes.length == 4) return   // keep-alive
    val lengthPrefix: Int = bytes.take(4).toByteBuffer.getInt
    val messageId: Byte = bytes.drop(4).head
    messageId match {
      case 0 => {   // choke
        am_choking = Choking
      }
      case 1 => {   // unchoke
        am_choking = NotChoking
      }
      case 2 => {   // interested
        peer_interested = Interested
      }
      case 3 => {   // not interested
        peer_interested = NotInterested
      }
      case 4 => {   // have
        val piece:Int = bytes.drop(5).take(4).toByteBuffer.getInt
        peer_pieces += piece
      }
      case 5 => {   // bitfield
        val bitfield:ByteString = bytes.drop(5).take(lengthPrefix-1)

        var counter:Int = 0
        for (byte:Byte <- bitfield) {
          for (mask <- Array(0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01)) {
            if ((byte & mask) > 0) peer_pieces += counter
            counter += 1
          }
        }
      }
      case 6 => {   // request
        println("I got a request message")
        val index = bytes.drop(5).take(4).toByteBuffer.getInt
        val begin = bytes.drop(9).take(4).toByteBuffer.getInt
        val length = bytes.drop(13).take(4).toByteBuffer.getInt
        watchers.foreach(x=>x!("request",index,begin,length))
      }
      case 7 => {   // piece
        continue_parsing = new PieceBuilder(metainfo,bytes)
      }
      case 8 => {   // cancel
        println("I got a cancel message")
      }
      case 9 => {   // port
        println("I got a port message")
      }
      case _ => {
        println("What message is this?")
        println("messageid = "+messageId)
        println("lengthprefix = "+lengthPrefix)
      }
    }
  }

  def takeInitiative: Unit = {
    if (connection == null) return
    if (acted_recently) {
      acted_recently = false
    } else if (am_choking == NotChoking) {
      continue_parsing = null
      requestPiece
      acted_recently = true
    } else {
      connection ! Tcp.Write(ByteString(keepAlive))
    }
  }

  def keepAlive:Array[Byte] = Array[Byte](0,0,0,0)

}

object Choke extends Enumeration {
  type Choke = Value
  val Choking, NotChoking = Value
}

object Interest extends Enumeration {
  type Interest = Value
  val Interested, NotInterested = Value
}