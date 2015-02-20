package bittorrent.client

import akka.actor.Actor
import akka.actor.ActorRef
import akka.io.IO
import akka.actor.ActorSystem
import akka.util.ByteString
import akka.actor.Props

import spray.can.Http
import spray.http.HttpResponse
import spray.http.StatusCodes

import scala.util.Random
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.concurrent.Future

import bittorrent.data.Metainfo

import bittorrent.pwp.{Pwp,Peer}
import bittorrent.pwp.Pwp._

import java.net.InetSocketAddress

import Pwp.Message
	
object Client {
	implicit val system: ActorSystem = ActorSystem("bittorrent")
	implicit val internet : ActorRef = IO(Http)
	val clientId = "-3d0000-"
	class HandShaker(implicit client:Client) extends Actor {
	
		override def receive = {
			case hs: HandShake =>
				client.handleHandshake(sender,hs)
			case _ =>
		}
	}
}

class Client(val port: Int = 6881) {
	import Pwp.Message
	import Client._
	private implicit val client: Client = this

	
	val socketAddress : InetSocketAddress = new InetSocketAddress(port)

	val pwp: ActorRef = system.actorOf(Props(new Pwp(socketAddress)),name = "Client-"+port.toString)
	
	system.actorOf(Props(new HandShaker),"hand_shaker-"+port.toString)

	val torrents: Map[ByteString,Torrent] = new HashMap

	val peerID = clientId + (0 to 11 map (x => Random.nextInt(10))).mkString

	private val peers: Map[String,Peer] = new HashMap

	def torrent(meta: Metainfo): Unit = {
		val handler = new Torrent(meta)(this)
		torrents.put(meta.infohash,handler)
	}
	private def handleHandshake(sender: ActorRef, hs: HandShake): Unit = { 
		val ih = torrents.get(hs.infohash)
		if(ih.isDefined){
			ih.get.addLeecher(hs.peerId,sender)
		} else {
			sender ! Close()
		}
	}
}
