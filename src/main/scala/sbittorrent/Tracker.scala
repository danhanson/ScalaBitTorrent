package sbittorrent
import akka.actor.{Actor, ActorRef}
import spray.http.HttpResponse

object Tracker {
	case class SendStarted(downloaded: Long, uploaded: Long)
	case class SendStopped(downloaded: Long, uploaded: Long)
	case class SendCompleted(downloaded: Long, uploaded: Long)
	object Status extends Enumeration {
		type Status = Value
		val Started, Stopped, Completed = Value
	}
}

class Tracker(val metainfo: Metainfo, val url: String) extends Actor {
	import Tracker.Status._
	import Tracker._

	override def receive = {
		case SendStarted(d,u) => sendRequest(Started,d,u,sender)
		case SendStopped(d,u) => sendRequest(Stopped,d,u,sender)
		case SendCompleted(d,u) => sendRequest(Completed,d,u,sender)
	}

	def sendRequest(): Unit = {
		sendRequest(Status.Started,0,0,self)
	}

	def sendRequest(status: Status, downloaded: Long, uploaded: Long, requester: ActorRef) = {
		val removeCookieHeaders: HttpResponse => HttpResponse =
			r => r.withHeaders(r.headers.filter(_.isNot("set-cookie")))
		//val pipeline = sendReceive ~> removeCookieHeaders
		//val pipeline: HttpRequest => Future[HttpResponse] = sendReceive()
	}


}