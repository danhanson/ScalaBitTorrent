package bittorrent.peer

import akka.actor.Actor
import java.net.InetAddress
import bittorrent.parser._

object Peer {
	def fromBNode(node: BNode): Peer = {
		node match {
			case s: StringNode => new Peer(s)
			case d: DictNode =>   new Peer(d)
			case _ => throw new IllegalArgumentException()
		}
	}
}

class Peer(val peerId:String,val address:InetAddress,val port:Int) extends Actor {

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
		this(node.value)
	}

	def this(node: StringNode) = {
		this(node.value)
	}

	override def receive = {
		case _ => println("PEER")
	}
}