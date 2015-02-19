package bittorrent.pwp

import akka.actor.Actor
import java.net.InetAddress
import bittorrent.parser._

object Peer {
	def fromList(list: List[DictNode]): Seq[Peer] = {
		list.map {
			peerDict => new Peer(peerDict)
		}
	}

	def fromCompactString(comp: String): Seq[Peer] = {
		println("compact string!")
		Nil
	}
}

class Peer(val peerId:String,val address:InetAddress,val port:Int) extends Actor {

	println(peerId)
	println(address)
	println(port)

	def this(peer:String,addrStr:String,portnum:Int) = {
		this(peer,InetAddress.getByName(addrStr),portnum)
	}

	def this(map: Map[String,BNode]) = {
		this(
			map.get("peer id").get.value.asInstanceOf[String],
			map.get("ip").get.value.asInstanceOf[String],
			map.get("port").get.value.asInstanceOf[Int]
		)
	}

	def this(comp: String) = {
		this(comp,comp.substring(5),comp.substring(5,6).toInt)
	}

	def this(node: DictNode) = {
		this("")
		// I couldn't merge this line because I couldn't figure out what you were trying to do
		//this(node.value)
	}

	def this(node: StringNode) = {
		this(node.value)
	}

	override def receive = {
		case _ => println("PEER")
	}
}