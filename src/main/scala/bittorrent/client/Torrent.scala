package bittorrent.client

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

import bittorrent.data.Metainfo
import bittorrent.data.Download

import scala.concurrent.Future
import scala.util.Try
import scala.collection.mutable.{Map,HashMap}

import spray.can.Http
import spray.http.HttpResponse

import akka.util.Timeout
import akka.actor.{Actor,ActorRef}
import akka.pattern.ask

import scala.concurrent.duration._

import java.util.concurrent.TimeUnit

import bittorrent.tracker._
import bittorrent.pwp._

import akka.io.IO

import scala.concurrent.ExecutionContext.Implicits.global

class Torrent(meta: Metainfo)(implicit client: Client) {
	import scala.concurrent.ExecutionContext.Implicits.global
	import Client._

	private implicit val timeout: Timeout = Timeout(2000)
	private implicit val handler = this;
	private implicit val download = new Download(meta)


	private val leechers : Map[String,ActorRef] = new HashMap
	private val seeders : Map[String,ActorRef] = new HashMap
	
	def port: Int = client.port
	def peerID : String = client.peerID

	def uploaded : Long = download.uploaded
	def downloaded : Long = download.downloaded
	def left : Long = download.left

	def state : Event = Started
	def compact : Int = 1
	val tracker: ActorRef = system.actorOf(Props(new Tracker(meta.announce)))
	
	implicit private def sendRequest(): Unit = {
		val res = (tracker ? new TrackerRequest(meta)).mapTo[TrackerResponse]

		res.onComplete {
			x => if(x.isSuccess)
				handleResponse(x.get)
			else
				handleError(x)
		}
	}

	private def handleResponse(response: TrackerResponse): Unit = {
		if(response.hasFailed){
			throw new Exception("Bad Response: "+response.failureReason)
		}
		val interval = Math.max(response.minInterval,response.interval)
		system.scheduler.scheduleOnce(Duration.create(interval,TimeUnit.SECONDS))(sendRequest)
	}

	private def handleError(t : Try[TrackerResponse]): Unit = {
		throw new Exception("THE TORRENT BROKE")
	}

	def addLeecher(peerId: String, ref: ActorRef){
		leechers.synchronized {
			if(leechers.contains(peerId)){
				return
			}
			leechers.put(peerId,ref)
		}
	}
}

trait FileStatus

abstract sealed class Event(val string: String){
	implicit override def toString(): String = string
}

case object Started extends Event("started")

case object Stopped extends Event("stopped")

case object Completed extends Event("completed")
