import java.math.BigInteger
import java.net.{InetAddress, URL}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.SecureRandom

import akka.actor.Actor
import spray.client.pipelining._
import spray.http.Uri.ParsingMode
import spray.http._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class TrackerCommunicator(val metainfo: Metainfo) extends Actor {
	var event: String = "started"
	val port: Int = 6881
	var uploaded: Int = 0
	var downloaded: Int = 0
	var left: Int = metainfo.fileLengths.values.sum
	val compact: Int = 1
	val no_peer_id: Int = 0
	val encodedInfoHash: String = URLUtil.toURLString(metainfo.infohash)
	val random = new SecureRandom
	val peer_id: String = new BigInteger(100, random).toString(32)
	var peer_list: List[(InetAddress, Int)] = List.empty[(InetAddress,Int)]		// (ip,port)
	var interval: Int = 0
	var complete: Int = 0
	var incomplete: Int = 0
	contactTracker

	private def contactTracker(): Unit = {
		val uri = Uri(createURL.toString, Charset.forName("ISO-8859-1"), ParsingMode.RelaxedWithRawQuery)
		val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
		pipeline(Get(uri)) onComplete {
			case Success(response: HttpResponse) => {
				val content = response.entity.asString(HttpCharsets.`ISO-8859-1`)
				println(content)
				val parsed = Decode.readDict(content.tail)._1
				interval = parsed.value.get("interval").get.asInstanceOf[IntNode].value
				complete = parsed.value.get("complete").get.asInstanceOf[IntNode].value
				incomplete = parsed.value.get("incomplete").get.asInstanceOf[IntNode].value
				val peer_list_buffer: ListBuffer[(InetAddress, Int)] = new ListBuffer[(InetAddress,Int)]
				parsed.value.get("peers").get match {
					case peers:ListNode => {
						// list of dictionaries, could be empty
						for (dict: BNode <- peers.value) {
							val peerMap = dict.asInstanceOf[DictNode].value
							val port = peerMap.get("port").get.asInstanceOf[IntNode].value
							val ipString: String = peerMap.get("ip").get.asInstanceOf[StringNode].value
							val address = InetAddress.getByName(ipString)
							peer_list_buffer += ((address,port))
						}
						println("GOT HERE")
					}
					case peers:StringNode => {
						// 6 bytes per peer => 4 bytes IP | 2 bytes port
						val byteArray = peers.value.getBytes("ISO-8859-1")
						for (i <- 0 to byteArray.length / 6 - 1) {
							val ipBytes = byteArray.slice(6 * i, 6 * i + 4)
							val portBytes = byteArray.slice(6 * i + 4, 6 * i + 6)
							val address: InetAddress = InetAddress.getByAddress(ipBytes)
							val port: Int = ByteBuffer.wrap(portBytes).getShort.toInt
							peer_list_buffer += ((address,port))
						}
					}
				}
				println("Peers are: " + peer_list_buffer.toList)
				peer_list = peer_list_buffer.toList
			}
			case Failure(response: HttpResponse) => {
				println("Tracker communication failed:")
				println(response)
			}
		}

	}

	private def createURL: URL = {
		new URL(metainfo.announce+
						"?event="+event+
						"&info_hash="+encodedInfoHash+
						"&peer_id="+peer_id+
						"&port="+port.toString+
						"&uploaded="+uploaded+
						"&downloaded="+downloaded+
						"&left="+left+
						"&compact="+compact+
						"&no_peer_id="+no_peer_id)
	}

	override def receive: Receive = {
		case "contact" => {
			contactTracker
		}
		case x => {
			println("Client received unknown message "+x)
		}
	}
}
