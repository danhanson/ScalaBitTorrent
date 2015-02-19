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

import bittorrent.metainfo.Metainfo

object Client {
	implicit val system: ActorSystem = ActorSystem()
	implicit val internet : ActorRef = IO(Http)
	val clientID = "-3d0000-"
}

class Client(val port: Int = 6881) {

	import Client._

	val files = new HashMap[String,TorrentFileHandler]

	val peerID = clientID + (0 to 11 map (x => Random.nextInt(10))).mkString

	def torrent(meta: Metainfo): Unit = {
		val handler = new TorrentFileHandler(meta)(this)
		files.put(meta.infohash,handler)
	}
}
