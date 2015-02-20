package bittorrent.tracker

import spray.http.HttpRequest
import spray.http.Uri
import spray.http.Uri
import spray.http.Uri.Query
import spray.http.Uri.Query.Cons
import spray.http.HttpMethods.GET
import bittorrent.data.Metainfo

import bittorrent.client._

object TrackerRequest {

}

case class TrackerRequest(meta: Metainfo)(implicit handler:Torrent) {
	import TrackerRequest._
	val infoHash = meta.infohash
	def peerID: String = handler.peerID
	def port: Int = handler.port
	def uploaded: Long = handler.uploaded
	def downloaded: Long = handler.downloaded
	def event: Event = handler.state

	implicit def toHttpRequest(trackerId: Option[String] = None): HttpRequest = {
		val tracker = Uri(meta.announce)
		val builder = Query.newBuilder ++= Seq[(String,String)](
				("peer_id",    handler.peerID),
				("port",       handler.port.toString),
				("uploaded",   handler.uploaded.toString),
				("downloaded", handler.downloaded.toString),
				("left",       handler.left.toString),
				("compact",    handler.compact.toString),
				("no_peer_id", 0.toString),
				("event",      handler.state.toString)
		)
		if(trackerId.isDefined){
			builder += (("trackerid",trackerId.get.toString))
		}
		val query = Query.Raw(builder.result().toString()+"&info_hash="+meta.encodedInfohash)
		Uri(tracker.scheme,tracker.authority,tracker.path,query,tracker.fragment)
		val uri = Uri(
				tracker.scheme,
				tracker.authority,
				tracker.path,
				if(trackerId.isDefined)
					Cons("trackerid",trackerId.get,query)	
				else
					query
				,tracker.fragment
			)
		val req = new HttpRequest(GET,uri)
		println(req.uri.toString())
		return req;
	}
}
