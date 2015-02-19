package bittorrent.client

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

	implicit val timeout: Timeout = new Timeout(2000)

	private var id = ""

	def trackerID: String = id

	override def receive = {
		case req: TrackerRequest =>
			val future : Future[HttpResponse] = (internet ? req.toHttpRequest(id)).mapTo[HttpResponse]
			future.onComplete {
				x =>
					val res: TrackerResponse = new TrackerResponse(x.get)
					if(res.trackerID != null){
						id = res.trackerID
					}
					sender ! res
			}
		case _ => throw new Exception("WHAT THE HELL IS THAT?")
	}
}