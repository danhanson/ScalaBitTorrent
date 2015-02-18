package bittorrent.client

import akka.actor.Actor
import akka.actor.ActorRef

import bittorrent.metainfo.Metainfo

import scala.concurrent.Future
import scala.util.Try

import spray.can.Http
import spray.http.HttpResponse

import akka.util.Timeout
import akka.actor.Actor
import akka.pattern.ask

import akka.io.IO

class TorrentFileHandler(meta: Metainfo)(implicit client: Client){
	import scala.concurrent.ExecutionContext.Implicits.global
	import Client._
	implicit val timeout: Timeout = Timeout(2000)
	implicit val handler = this;
	def port: Int = client.port
	def peerID : String = client.peerID
	def uploaded : Long = 0
	def downloaded : Long = 0
	def state : Event = Started
	val tracker: ActorRef = new Tracker(meta.announce).self
	
	private val res: Future[TrackerResponse] =
		(tracker ? new TrackerRequest(meta)).mapTo[TrackerResponse]

	res.onComplete {
		x => if(x.isSuccess)
			handleResponse(x.get)
		else
			handleError()
	}

	def handleResponse(response: TrackerResponse): Unit = {
		
	}

	def handleError(): Unit = {
		throw new Exception("THE TORRENT BROKE")
	}
}

trait FileStatus

abstract sealed class Event(val string: String){
	implicit override def toString(): String = string
}

case object Started extends Event("started")

case object Stopped extends Event("stopped")

case object Completed extends Event("completed")
