package bittorrent.metainfo

import spray.http.Uri

class Metainfo {
	val announce : Uri = Uri()
	val info : InfoDictionary = new InfoDictionary()
}