package bittorrent.client

import spray.http.HttpRequest
import spray.http.Uri
import spray.http.Uri
import spray.http.Uri.Query
import spray.http.Uri.Query.Cons
import spray.can.Http
import spray.http.HttpMethods.GET

import java.security.MessageDigest

import bittorrent.metainfo.Metainfo

object TrackerRequest {
	private def uri(meta: Metainfo)(implicit handler:TorrentFileHandler): Uri = {
		val params = List[(String,String)](
				("info_hash",  meta.infoHash),
				("peer_id",    handler.peerID),
				("port",       handler.port.toString),
				("uploaded",   handler.uploaded.toString),
				("downloaded", handler.downloaded.toString),
				("event",      handler.state.toString)
		)
		val tracker = Uri(meta.announce)
		val query = params.foldLeft[Query](tracker.query)((q: Query, kv) => Cons(kv._1,kv._2,q))
		Uri(tracker.scheme,tracker.authority,tracker.path,query,tracker.fragment)
	}

}

case class TrackerRequest(meta: Metainfo)(implicit handler:TorrentFileHandler) {
	import TrackerRequest._
	val infoHash = meta.infoHash
	def peerID: String = handler.peerID
	def port: Int = handler.port
	def uploaded: Long = handler.uploaded
	def downloaded: Long = handler.downloaded
	def event: Event = handler.state

	implicit def toHttpRequest(trackerID: String = null): HttpRequest = {
		val params = Seq[(String,String)](
				("info_hash",  infoHash),
				("peer_id",    peerID),
				("port",       port.toString),
				("uploaded",   uploaded.toString),
				("downloaded", downloaded.toString),
				("event",      event.toString)
		)
		
		val tracker = Uri(meta.announce)
		val query = params.foldLeft[Query](tracker.query)((q: Query, kv) => Cons(kv._1,kv._2,q))
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
		new HttpRequest(GET,uri)
	}
}
