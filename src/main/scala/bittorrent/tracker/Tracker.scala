package bittorrent.tracker

import scala.concurrent.Future

import akka.actor.Actor
import akka.actor.ActorRef
import akka.util.Timeout

import akka.pattern.ask
import spray.http.Uri

import spray.http.HttpResponse
import spray.http.HttpRequest

class Tracker(val uri: Uri)(implicit internet: ActorRef) extends Actor {
	import context.dispatcher

	implicit val timeout: Timeout = new Timeout(100000)

	private var id: Option[String] = None

	def trackerId: String = id.get

	override def receive = {
		case req: TrackerRequest =>
			println("request sent to tracker")
			val torrent = sender
			val future : Future[HttpResponse] = (internet ? req.toHttpRequest(id)).mapTo[HttpResponse]
			future.onComplete {
				x =>
					println("completed")
					val res: TrackerResponse = new TrackerResponse(x.get)
					if(res.hasTrackerId){
						id = Option(res.trackerId)
					}
					torrent ! res
			}
		case _ => throw new Exception("WHAT THE HELL IS THAT?")
	}
}
