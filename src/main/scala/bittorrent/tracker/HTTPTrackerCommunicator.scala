package bittorrent.tracker

import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import bittorrent.data.{URLUtil, Metainfo}
import bittorrent.parser._
import bittorrent.peer.PeerCommunicationManager
import spray.client.pipelining._
import spray.http.Uri.ParsingMode
import spray.http._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class TrackerStatusUpdate(var id:Int,val incomplete:Int,val complete:Int,val peers:Int)

/*  Responsible for communication with the tracker initially and then at regular
 *  intervals. It maintains peer_list which contains all of the peers from the
 *  tracker. After the initial communication, it spawns an actor to deal with
 *  managing the peer workers that will need to be spawned for the peer list.
 *
 *  As of 2/19/2015 it supports HTTP but not UDP
 */
class HTTPTrackerCommunicator(val metainfo:Metainfo,val my_announce:String,id:Int) extends Actor {
	var event: String = "started"
	val port: Int = 6881
	var uploaded: Int = 0
	var downloaded: Int = 0
	var left: Long = metainfo.totalLength
	val compact: Int = 1
	val no_peer_id: Int = 0
	val encodedInfoHash: String = URLUtil.toURLString(metainfo.infohash)
	val random = new SecureRandom
	val peer_id: String = new BigInteger(100, random).toString(32)
	var peer_list: List[(InetAddress, Short)] = List.empty[(InetAddress,Short)]		// (ip,port)
	var interval: Int = 0
	var complete: Int = 0
	var incomplete: Int = 0
	var connectionId:Long = 0x0000041727101980L
	var listeners:ListBuffer[ActorRef] = new ListBuffer[ActorRef]
	var peer_manager:ActorRef = null

	contactTracker


	private def contactTracker(): Unit = {
		if (my_announce.startsWith("http")) {
			contactHTTPTracker
		} else {
			println("Unrecognized protocol: "+my_announce)
		}
	}

	private def contactHTTPTracker(): Unit = {
		val uri = Uri(createURL, Charset.forName("ISO-8859-1"), ParsingMode.RelaxedWithRawQuery)

		val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
		pipeline(Get(uri)) onComplete {
			case Success(response: HttpResponse) => {
				val content = response.entity.asString(HttpCharsets.`ISO-8859-1`)
				val parsed = Decode.readDict(content.tail)._1
				interval = parsed.value.get("interval").get.asInstanceOf[IntNode].value
				context.system.scheduler.scheduleOnce(Duration.create(interval,TimeUnit.SECONDS))(contactTracker)
				complete = parsed.value.get("complete").get.asInstanceOf[IntNode].value
				incomplete = parsed.value.get("incomplete").get.asInstanceOf[IntNode].value
				val peer_list_buffer: ListBuffer[(InetAddress, Short)] = new ListBuffer[(InetAddress,Short)]
				println(uri)
				println(content)
				println(parsed)
				parsed.value.get(parsed.value.keys.filter(x=>x.startsWith("peers")).head).get match {
					case peers: ListNode => {
						// list of dictionaries, could be empty
						for (dict: BNode <- peers.value) {
							val peerMap = dict.asInstanceOf[DictNode].value
							val port = peerMap.get("port").get.asInstanceOf[IntNode].value
							val ipString: String = peerMap.get("ip").get.asInstanceOf[StringNode].value
							val address = InetAddress.getByName(ipString)
							peer_list_buffer += ((address, port.toShort))
						}
					}
					case peers: StringNode => {
						// 6 bytes per peer => 4 bytes IP | 2 bytes port
						val byteArray = peers.value.getBytes("ISO-8859-1")
						for (i <- 0 to byteArray.length / 6 - 1) {
							val ipBytes = byteArray.slice(6 * i, 6 * i + 4)
							val portBytes = byteArray.slice(6 * i + 4, 6 * i + 6)
							val address: InetAddress = InetAddress.getByAddress(ipBytes)
							val port: Short = ByteBuffer.wrap(portBytes).getShort
							if (port > 0) peer_list_buffer += ((address, port))
						}
					}
				}
				peer_list = peer_list_buffer.toList
				notifyObservers
				startPeerCommunication
			}
			case x => {
				println("Tracker communication failed: "+x)
			}
		}
	}

	private def startPeerCommunication: Unit = {
		if (peer_manager == null) {
			peer_manager = context.actorOf(Props(
				new PeerCommunicationManager(metainfo,peer_id.getBytes,id)),
				name="peerCommunicationManager"+id)
			listeners.foreach(x => x ! peer_manager)
		}
		peer_manager ! peer_list.toList
	}

	private def notifyObservers: Unit = {
		var update = new TrackerStatusUpdate(id,incomplete,complete,peer_list.length)
		listeners.foreach(x => x ! update)
	}


	private def createURL: String = {
		return my_announce+
			"?event="+event+
			"&info_hash="+encodedInfoHash+
			"&peer_id="+peer_id+
			"&port="+port.toString+
			"&uploaded="+uploaded+
			"&downloaded="+downloaded+
			"&left="+left+
			"&compact="+compact+
			"&no_peer_id="+no_peer_id+
			"&supportcrypto=0"
	}

	override def receive: Receive = {
		case "contact" => contactTracker
		case "subscribe" => listeners += sender
		case peerManager:ActorRef => peer_manager = peerManager
		case x => println("Client received unknown message: "+x)
	}

}