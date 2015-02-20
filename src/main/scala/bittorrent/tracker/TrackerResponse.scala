package bittorrent.tracker

import spray.http.HttpResponse
import bittorrent.parser._
import bittorrent.client.Torrent

import bittorrent.pwp._

object TrackerResponse {
	private def get[T](dict: Map[String,BNode],key:String): Option[T] = {
		dict.get(key).map { x => x.value.asInstanceOf[T] }
	}
}

class TrackerResponse(res: HttpResponse) {
	import TrackerResponse._
	println(res.entity.asString)
	private val dict = Decode.asDictionary(res.entity.asString)

	// TODO: handle caste exceptions caused by bad responses

	private val failureReasonOpt: Option[String] = get[String](dict,"failure reason")

	val warningMessageOpt: Option[String] = get[String](dict,"warning message")

	val interval: Int = get[Int](dict,"interval").getOrElse(2)

	val minInterval: Int = get[Int](dict,"interval").getOrElse(0)

	private val trackerIdOpt: Option[String] = get[String](dict,"tracker id")

	private def peers(implicit torrent: Torrent) = {
		dict.get("peers").map {
			x => x match {
				case l: ListNode => Peer.fromList(l.value.asInstanceOf[List[DictNode]])
				case s: StringNode => Peer.fromCompactString(s.value)
				case _ => throw new IllegalArgumentException()
			}
		}
	}

	override def toString = res.status.toString() + " "+res.protocol+" "+res.entity

	val hasTrackerId : Boolean = trackerIdOpt.isDefined
	lazy val trackerId : String = trackerIdOpt.get
	
	val hasFailed : Boolean = failureReasonOpt.isDefined
	lazy val failureReason : String = failureReasonOpt.get

	val hasWarning : Boolean = warningMessageOpt.isDefined
	lazy val warning : String = warningMessageOpt.get
}
