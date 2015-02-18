package bittorrent.client

import spray.http.HttpResponse
import bittorrent.parser.Decode


class TrackerResponse(res: HttpResponse) {
	val trackerID: String = "EXTRACT TRACKER ID FROM HTTP REQUEST"
	private val dict = Decode.readDict(res.entity.asString)
	private val 
}
