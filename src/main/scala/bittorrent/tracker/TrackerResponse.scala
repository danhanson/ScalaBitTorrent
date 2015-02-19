package bittorrent.tracker

import spray.http.HttpResponse
import bittorrent.parser._

import bittorrent.peer._

object TrackerResponse {
	private def get[T](dict: Map[String,BNode],key:String, default:T = null): T = {
		dict.get(key).map { x => x.value.asInstanceOf[T] }.getOrElse(default)
	}
}

class TrackerResponse(res: HttpResponse) {
	import TrackerResponse._

	private val dict = Decode.readDict(res.entity.asString.tail)._1.value

	val failureReason: String = get[String](dict,"failure reason")
	val warningMessage: String = get[String](dict,"warning message")
	val interval: Int = get[Int](dict,"interval",0)
	val minInterval: Int = get[Int](dict,"interval",0)
	val trackerID: String = get[String](dict,"tracker id")
	def peers: Seq[Peer] = Nil
}
