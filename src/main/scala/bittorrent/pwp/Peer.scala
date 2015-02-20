package bittorrent.pwp

import akka.actor.Actor
import java.net.InetAddress
import bittorrent.parser._
import bittorrent.data.HasPieces
import bittorrent.client.Torrent
import bittorrent.pwp.Pwp._
import scala.collection.mutable

object Peer {
	def fromList(list: List[DictNode])(implicit torrent:Torrent): Seq[Peer] = {
		list.map {
			peerDict => new Peer(peerDict)
		}
	}

	def fromCompactString(comp: String)(implicit torrent:Torrent): Seq[Peer] = {
		if(comp.length % 6 > 0) throw new IllegalArgumentException
		(0 until (comp.length/6)).map {
			x => new Peer(comp.slice(x*6, x*(6+1)))
		}
	}
}

class Peer(val peerId:String,val address:InetAddress,val port:Int)(implicit torrent: Torrent) extends Actor with HasPieces {

	override val pieces = torrent.metafile.pieces	

	private var hasPieces: mutable.Seq[Boolean] = new Array[Boolean](torrent.metafile.pieces.length)

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

	def this(comp: String)(implicit torrent:Torrent) = {
		this(comp,comp.substring(5),comp.substring(5,6).toInt)
	}

	def this(node: DictNode)(implicit torrent:Torrent) = {
		this(node.value)
	}

	def this(node: StringNode)(implicit torrent:Torrent) = {
		this(node.value)
	}

	override def receive = {
		case Have(index) => hasPieces(index) = true
		case _ => println("PEER")
	}
}