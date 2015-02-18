package bittorrent.client

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import spray.http.Uri

class Tracker(val uri: Uri)(implicit internet: ActorRef) extends Actor {

	implicit val timeout: Timeout = new Timeout(2000)

	private var id = ""

	def trackerID: String = id

	override def receive = {
		//case req: TrackerRequest =>
		//	val future : Future[HttpResponse] = (internet ? req.toHttpRequest(id)).mapTo[HttpResponse]
		//	future.onComplete { x => sender ! new TrackerResponse(x.get) }
		case _ => throw new Exception("WHAT THE HELL IS THAT?")
	}
}