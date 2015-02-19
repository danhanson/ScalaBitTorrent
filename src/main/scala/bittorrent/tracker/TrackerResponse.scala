package bittorrent.tracker

import spray.http.HttpResponse
import bittorrent.parser._

import bittorrent.peer._

object TrackerResponse {
	private def get[T](dict: Map[String,BNode],key:String): Option[T] = {
		dict.get(key).map { x => x.value.asInstanceOf[T] }
	}
}

class TrackerResponse(res: HttpResponse) {
	import TrackerResponse._
	println(res.entity.asString)
	private val dict = Decode.asDictionary(res.entity.asString)

	private val failureReason: Option[String] = get[String](dict,"failure reason")
	val warningMessageOpt: Option[String] = get[String](dict,"warning message")
	val interval: Int = get[Int](dict,"interval").getOrElse(2)
	val minInterval: Int = get[Int](dict,"interval").getOrElse(0)
	private val trackerIdOpt: Option[String] = get[String](dict,"tracker id")

	private val peers: Option[Seq[Peer]] = get[List[BNode]](dict,"peers").map {
		x => x.map { node => Peer.fromBNode(node) }
	}

	val hasTrackerId : Boolean = trackerIdOpt.isDefined
	lazy val trackerId : String = trackerIdOpt.get
}
