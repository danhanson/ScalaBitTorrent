package sbittorrent
import java.security.MessageDigest

object TrackerRequest {

	val sha1Encoder = MessageDigest.getInstance("SHA-1")
/*
	private def makeInfoHash(meta: Metainfo): String = {
		//sha1Encoder.digest(meta.info.toString().getBytes("UTF-8")).toString()
	}

	private def uri(meta: Metainfo)(implicit handler:TorrentFileHandler): Uri = {
		val params = List[(String,String)](
				("info_hash",  makeInfoHash(meta)),
				("peer_id",    handler.peerID),
				("port",       handler.port.toString),
				("uploaded",   handler.uploaded.toString),
				("downloaded", handler.downloaded.toString),
				("event",      handler.state.toString)
		)
		val tracker = meta.announce
		//val query = params.foldLeft[Query](tracker.query)((q: Query, kv) => Cons(kv._1,kv._2,q))
		//Uri(tracker.scheme,tracker.authority,tracker.path,query,tracker.fragment)
	}
*/
}
/*
case class TrackerRequest(meta: Metainfo)(implicit handler:TorrentFileHandler) {
	//val infoHash = makeInfoHash(meta)
	def peerID: String = handler.peerID
	def port: Int = handler.port
	def uploaded: Long = handler.uploaded
	def downloaded: Long = handler.downloaded
	def event: Event = handler.state
/*
	implicit def toHttpRequest(trackerID: String = ""): HttpRequest = {
		val params = Seq[(String,String)](
				("info_hash",  infoHash),
				("peer_id",    peerID),
				("port",       port.toString),
				("uploaded",   uploaded.toString),
				("downloaded", downloaded.toString),
				("event",      event.toString)
		)
		
		val tracker = meta.announce
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
	*/
}
*/