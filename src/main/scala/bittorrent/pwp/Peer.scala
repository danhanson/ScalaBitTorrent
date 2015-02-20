package bittorrent.pwp

import akka.actor.{Actor,Props,ActorRef}
import java.net.InetAddress
import java.net.Inet4Address
import bittorrent.parser._
import bittorrent.data.HasPieces
import bittorrent.client.Torrent
import bittorrent.pwp.Pwp._
import scala.collection.mutable
import java.nio.ByteBuffer
import java.net.Inet4Address

object Peer {
	def fromList(list: List[DictNode])(implicit torrent:Torrent): Seq[Peer] = {
		list.map {
			peerDict => new Peer(peerDict)
		}
	}

	def fromCompactString(comp: String)(implicit torrent:Torrent): Seq[Peer] = {
		if(comp.length % 6 > 0) throw new IllegalArgumentException
		val bytes = comp.getBytes("ISO-8859-1")
		(0 until (bytes.length/6)).map {
			x => new Peer(bytes.slice(6*x,6*x + 6))
		}
	}

	class HaveReceiver(peer: Peer) extends Actor {
		override def receive = {
			case Have(index) => peer.hasPieces(index) = true
			case _ => println("PEER")
		}
	}
}

class Peer(val peerId:String,val address:InetAddress,val port:Int)(implicit torrent: Torrent) extends HasPieces {
	import Peer._

	override val pieces = torrent.metafile.pieces	

	private var hasPieces: mutable.Seq[Boolean] = new Array[Boolean](torrent.metafile.pieces.length)

	private var haveReceiver: ActorRef = torrent.getSystem.actorOf(Props(new HaveReceiver(this)))
	
	override def hasPiece = hasPieces.toSeq

	def this(peer:String,addrStr:String,portnum:Int)(implicit torrent:Torrent) = {
		this(peer,InetAddress.getByName(addrStr),portnum)
	}

	def this(map: Map[String,BNode])(implicit torrent:Torrent) = {
		this(
			map.get("peer id").get.value.asInstanceOf[String],
			map.get("ip").get.value.asInstanceOf[String],
			map.get("port").get.value.asInstanceOf[Int]
		)
	}

	def this(comp: Seq[Byte])(implicit torrent:Torrent) = {
		this(
			comp.toString,
			InetAddress.getByAddress(comp.slice(0,4).toArray),
			ByteBuffer.wrap(comp.slice(4,6).toArray).getShort
		)
	}

	def this(node: DictNode)(implicit torrent:Torrent) = {
		this(node.value)
	}

	override def toString = {
		peerId+"@"+address.toString()
	}
}