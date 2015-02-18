import java.math.BigInteger
import java.net.{InetAddress, URL, URLConnection}
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util

import akka.actor.Actor
import org.apache.commons.io.IOUtils
import sun.net.www.content.text.PlainTextInputStream

class ClientActor(val metainfo: Metainfo) extends Actor {
	var event = "started"
	val port = 6881
	var uploaded = 0
	var downloaded = 0
	var left = metainfo.fileLengths.values.sum
	val compact = 1
	val no_peer_id = 0
	val encodedInfoHash: String = URLUtil.toURLString(metainfo.infohash)
	val random = new SecureRandom
	val peer_id = new BigInteger(100, random).toString(32)
	communicate

	private def communicate(): Int = {
		val connect: URLConnection = createURL.openConnection
		val response = connect.getContent
		response match {
			case text: PlainTextInputStream => {
				val content = IOUtils.toString(text, "ISO-8859-1")
				println(content)
				val parsed = Decode.readDict(content.tail)._1
				val interval = parsed.value.get("interval").get.asInstanceOf[IntNode].value
				val complete = parsed.value.get("complete").get.asInstanceOf[IntNode].value
				val incomplete = parsed.value.get("incomplete").get.asInstanceOf[IntNode].value
				println("Complete: "+complete)
				println("Incomplete: "+incomplete)
				println("Interval: "+interval)
				parsed.value.get("peers").get match {
					case peers:ListNode => {
						// list of dictionaries, could be empty
					}
					case peers:StringNode => {
						// 6 bytes per peer
						// 4 bytes IP | 2 bytes port
						val byteArray = peers.value.getBytes("ISO-8859-1")
						println("byteArray.length = "+byteArray.length)
						for (i <- 0 to byteArray.length / 6 - 1) {
							var ipBytes = byteArray.slice(6*i,6*i+4)
							var portBytes = byteArray.slice(6*i+4,6*i+6)
							println("ipBytes: "+util.Arrays.toString(ipBytes))
							println("portBytes: "+util.Arrays.toString(portBytes))
							var address = InetAddress.getByAddress(ipBytes)
							var port: Short = ByteBuffer.wrap(portBytes).getShort
							println("Address: "+address)
							println("Port: "+port)
						}
					}
				}
				0
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
						"&numwant=4"+
						"&no_peer_id="+no_peer_id)
	}

	override def receive: Receive = {
		null
	}
}
