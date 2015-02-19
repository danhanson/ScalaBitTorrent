package bittorrent.client

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import bittorrent.metainfo.Metainfo

import scala.concurrent.Future
import scala.util.Try

import spray.can.Http
import spray.http.HttpResponse

import akka.util.Timeout
import akka.actor.Actor
import akka.pattern.ask

import bittorrent.tracker._
import bittorrent.peer._

import akka.io.IO

class TorrentFileHandler(meta: Metainfo)(implicit client: Client){
	import scala.concurrent.ExecutionContext.Implicits.global
	import Client._

	implicit val timeout: Timeout = Timeout(2000)
	private implicit val handler = this;


	private var trackerID : Option[String] = None

	def port: Int = client.port
	def peerID : String = client.peerID
	def uploaded : Long = 0
	def downloaded : Long = 0
	def left : Long = meta.fileLengths.values.sum
	def state : Event = Started
	def compact : Int = 1
	val tracker: ActorRef = system.actorOf(Props(new Tracker(meta.announce)))
	
	private val res: Future[TrackerResponse] =
		(tracker ? new TrackerRequest(meta)).mapTo[TrackerResponse]

	res.onComplete {
		x => if(x.isSuccess)
			handleResponse(x.get)
		else
			handleError(x)
	}

	private def handleResponse(response: TrackerResponse): Unit = {
		
	}

	private def handleError(t : Try[TrackerResponse]): Unit = {
		throw new Exception("THE TORRENT BROKE")
	}

	private def hasTrackerId: Boolean = {
		trackerID.isDefined
	}

	private def trackerId: String = {
		trackerID.get
	}
}

trait FileStatus

abstract sealed class Event(val string: String){
	implicit override def toString(): String = string
}

case object Started extends Event("started")

case object Stopped extends Event("stopped")

case object Completed extends Event("completed")
