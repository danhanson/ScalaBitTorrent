package bittorrent.data

import akka.util.ByteString

object Download {

	object BitField {
		def bytesFromDownload(download: HasPieces): Seq[Byte] = {
			val pieces = download.pieces
			val bytes = new Array[Byte](pieces.length)
			for(i <- (0 until pieces.length)){
				val byte = i / 8
				val bit = (1 << (i % 8)).toByte
				if(download.hasPiece(i)){
					bytes(byte) = (bytes(byte) | bit).toByte
				}
			}
			bytes
		}
	}

	class BitField(val bytes: Seq[Byte]){

		def this(download: HasPieces) = {
			this(BitField.bytesFromDownload(download))
		}

		def hasPiece(piece: Piece): Boolean = {
			val byte = piece.index / 8
			val bit = 1 << (piece.index % 8)
			(bytes(byte) & bit) == 0x01
		}
	}

	class Block(val index:Int,val begin:Int,val length:Int)(implicit meta: Metainfo)

	class Piece(val index:Int,val length:Int,val sha1:Seq[Byte])(implicit meta: Metainfo){
		def getBlock(begin:Int,length:Int){
			new Block(index,begin,length)
		}
	}
}

trait HasPieces {
	import Download._

	val pieces: Seq[Piece]
	
	def hasPiece: Seq[Boolean]

	def hasPiece(piece: Piece): Boolean = {
		hasPiece(piece.index)
	}

	def getBitfield(): BitField = {
		new BitField(this)
	}
}

class Download(val meta: Metainfo) extends HasPieces {
	import Download._

	private var uploadedP = 0
	private var downloadedP = 0
	private var leftP = meta.totalLength

	val pieces = meta.pieces

	def left: Long = leftP
	def uploaded: Long = uploadedP
	def downloaded: Long = downloadedP

	private var hasPieces: Seq[Boolean] = new Array[Boolean](pieces.length)

	def hasPiece = hasPieces
	
	def hasBlock(block: Block): Boolean = {
		false
	} 

	def getBlock(block: Block): ByteString = {
		null
	}
}