package bittorrent.tracker

import java.math.BigInteger
import java.net.{InetAddress, DatagramSocket, InetSocketAddress}
import java.security.SecureRandom

import akka.actor.{Props, Actor, ActorRef}
import akka.io.UdpConnected._
import akka.io.{IO, UdpConnected}
import akka.util.ByteString
import bittorrent.data.Metainfo
import bittorrent.peer.PeerCommunicationManager
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import spray.http.Uri

import scala.collection.mutable.ListBuffer
import scala.util.Random

class UDPTrackerCommunicator(val metainfo:Metainfo, id:Int) extends Actor {
  import context.system
  val rnd = new SecureRandom
  val port:Short = 6881
  var connection_id:Long = 0x41727101980L
  val transaction_id = rnd.nextInt
  val peer_id: String = new BigInteger(100, rnd).toString(32)
  var connected:Boolean = false
  var announced:Boolean = false
  var downloaded:Long = 0L
  var uploaded:Long = 0L
  var left:Long = metainfo.fileLength
  var interval:Int = -1
  var leechers:Int = -1
  var seeders:Int = -1
  val peer_list = new ListBuffer[(InetAddress,Short)]
  var peer_manager:ActorRef = null
  val listeners = new ListBuffer[ActorRef]

  val remote_host = metainfo.announce.drop(6).split(':')(0)
  val remote_port:Short = metainfo.announce.drop(6).split(':')(1).split('/')(0).toShort
  val remote = new InetSocketAddress(remote_host,remote_port)

  val manager = IO(UdpConnected)
  manager ! Connect(self,remote)

  def receive:Receive = {
    case Connected => {
      context.become(ready(sender))
      self ! connectBytes
    }
    case "subscribe" => listeners += sender
    case x => println("UDPTrackerCommunicator received unknown: "+x)
  }

  def ready(connection:ActorRef):Receive = {
    case Received(data:ByteString) => parse(data)
    case msg: String => connection ! Send(ByteString(msg))
    case msg:ByteString => connection ! Send(msg)
    case Disconnect => connection ! UdpConnected.Disconnect
    case x => println("UDPTrackerCommunicator received unknown: "+x)
  }

  def parse(bytes:ByteString) = {
    if (!connected) {
      parseConnect(bytes)
      self ! announceBytes
    } else if (!announced) {
      parseAnnounce(bytes)
    } else {
      parseAnnounce(bytes)    // has some format
    }
  }

  def parseConnect(bytes:ByteString) {
    val action = bytes.take(4).asByteBuffer.getInt
    val transaction_id = bytes.drop(4).take(4).asByteBuffer.getInt
    connection_id = bytes.drop(8).take(8).asByteBuffer.getLong
    connected = true
  }

  def parseAnnounce(bytes:ByteString): Unit = {
    val action = bytes.take(4).asByteBuffer.getInt
    val transaction_id = bytes.drop(4).take(4).asByteBuffer.getInt
    interval = bytes.drop(8).take(4).asByteBuffer.getInt
    leechers = bytes.drop(12).take(4).asByteBuffer.getInt
    seeders = bytes.drop(16).take(4).asByteBuffer.getInt
    for (i <- 20 until bytes.length by 6) {
      val ip:InetAddress = InetAddress.getByAddress(bytes.drop(i).take(4).toArray)
      val port:Short = bytes.drop(i+4).take(2).asByteBuffer.getShort
      peer_list += ((ip,port))
    }
    announced = true
    notifyObservers
    if (peer_manager == null) startPeerCommunication
  }

  def connectBytes:ByteString = {
    val action = 0
    var bytes:ChannelBuffer = ChannelBuffers.buffer(16)
    bytes.writeLong(connection_id)
    bytes.writeInt(action)
    bytes.writeInt(transaction_id)
    ByteString(bytes.array)
  }

  def announceBytes:ByteString = {
    val action = 1  // announce
    val event = 0
    val ip = 0
    val key = 0     //  ???
    val num_want = -1
    var bytes:ChannelBuffer = ChannelBuffers.buffer(98)
    bytes.writeLong(connection_id)              //  8
    bytes.writeInt(action)                      //  4
    bytes.writeInt(transaction_id)              //  4
    bytes.writeBytes(metainfo.infohash.toArray) //  20
    bytes.writeBytes(peer_id.getBytes)          //  20
    bytes.writeLong(downloaded)
    bytes.writeLong(left)
    bytes.writeLong(uploaded)
    bytes.writeInt(event)
    bytes.writeInt(ip)
    bytes.writeInt(key)
    bytes.writeInt(num_want)
    bytes.writeShort(port)
    ByteString(bytes.array)
  }

  private def notifyObservers: Unit = {
    var update = new TrackerStatusUpdate(id,leechers,seeders,peer_list.length)
    listeners.foreach(x=>x!update)
  }

  private def startPeerCommunication: Unit = {
    peer_manager = context.actorOf(Props(
      new PeerCommunicationManager(metainfo,peer_id.getBytes,peer_list.toList,id)),
      name="peerCommunicationManager"+id)
    listeners.foreach(x => x ! peer_manager)
  }

}
