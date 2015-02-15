package bittorrent.client

import akka.actor.Actor

import bittorrent.metainfo.Metainfo

import scala.concurrent.Future
import scala.util.Try

import spray.can.Http
import spray.http.HttpResponse

import akka.util.Timeout
import akka.actor.Actor
import akka.pattern.ask

import akka.io.IO

class TorrentFileHandler(meta: Metainfo)(implicit client: Client) extends Actor {
	import context.dispatcher
	def port: Int = client.port
	def peerID : String = client.peerID
	def uploaded : Long = 0
	def downloaded : Long = 0
	def state : Event = Started
	implicit val timeout: Timeout = Timeout(2000)
	implicit val handler = this;
	implicit val system = Client.system
	
	private val res: Future[HttpResponse] = (Client.internet ? TrackerRequest(meta)).mapTo[HttpResponse]

	res.onComplete { x =>
		
	}

	def receive(res: Try[HttpResponse]): Unit = {
		if(res.isSuccess)
			this.handleResponse(res.get)
		else
			handleError
	}

	def handleResponse(response: HttpResponse): Unit = {
		
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
