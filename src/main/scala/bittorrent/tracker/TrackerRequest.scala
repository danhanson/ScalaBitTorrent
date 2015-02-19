package bittorrent.tracker

import spray.http.HttpRequest
import spray.http.Uri
import spray.http.Uri
import spray.http.Uri.Query
import spray.http.Uri.Query.Cons
import spray.http.HttpMethods.GET
import bittorrent.metainfo.Metainfo

import bittorrent.client._

object TrackerRequest {

}

case class TrackerRequest(meta: Metainfo)(implicit handler:TorrentFileHandler) {
	import TrackerRequest._
	val infoHash = meta.infohash
	def peerID: String = handler.peerID
	def port: Int = handler.port
	def uploaded: Long = handler.uploaded
	def downloaded: Long = handler.downloaded
	def event: Event = handler.state

	implicit def toHttpRequest(trackerID: String = null): HttpRequest = {
		val tracker = Uri(meta.announce)
		val builder = Query.newBuilder ++= Seq[(String,String)](
				("info_hash",  meta.infohash),
				("peer_id",    handler.peerID),
				("port",       handler.port.toString),
				("uploaded",   handler.uploaded.toString),
				("downloaded", handler.downloaded.toString),
				("left",       handler.left.toString),
				("compact",    handler.compact.toString),
				("no_peer_id", 0.toString),
				("event",      handler.state.toString)
		)
		if(handler.hasTrackerId){
			
		}
		val query = builder.result()
		Uri(tracker.scheme,tracker.authority,tracker.path,query,tracker.fragment)
		val uri = Uri(
				tracker.scheme,
				tracker.authority,
				tracker.path,
				if(trackerID == "")
					query
				else
					Cons("trackerid",trackerID,query)	
				,tracker.fragment
			)
		val req = new HttpRequest(GET,uri)
		println(req.uri.toString())
		return req;
	}
}
