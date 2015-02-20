package bittorrent.client

import akka.actor.Actor
import akka.actor.ActorRef
import akka.io.IO
import akka.actor.ActorSystem

import spray.can.Http
import spray.http.HttpResponse
import spray.http.StatusCodes

import scala.util.Random
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

import bittorrent.data.Metainfo

import bittorrent.pwp.Pwp
import bittorrent.pwp.Pwp._

import java.net.InetSocketAddress

object Client {
	implicit val system: ActorSystem = ActorSystem()
	implicit val internet : ActorRef = IO(Http)
	val peers = IO(Pwp)
	val clientId = "-3d0000-"
}

class Client(val port: Int = 6881) extends Actor {
	import Pwp.Message
	import Client._

	val socketAddress : InetSocketAddress = new InetSocketAddress(port)

	peers ! Bind(self,new InetSocketAddress(port))

	val files = new HashMap[String,Torrent]

	val peerID = clientId + (0 to 11 map (x => Random.nextInt(10))).mkString

	def torrent(meta: Metainfo): Unit = {
		val handler = new Torrent(meta)(this)
		files.put(meta.infohash,handler)
	}

	override def receive = {
		case hs: HandShake =>
			handleHandshake(sender,hs)
		case _ => println("asdsadaasdasd")
	}

	private def handleHandshake(sender: ActorRef, hs: HandShake): Unit = { 
		files.synchronized {
			val ih = files.get(hs.infohash)
			if(ih.isDefined){
				ih.get.addLeecher(hs.peerId,sender)
			}
		}
	}
}
