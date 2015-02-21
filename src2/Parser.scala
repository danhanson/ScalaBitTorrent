package peer

import akka.util.{ByteString, ByteStringBuilder}
import java.util.concurrent.LinkedBlockingQueue

class MessageParser{

	private val queue:LinkedBlockingQueue[Byte] = new LinkedBlockingQueue

	private val constantMessages = Map(0 -> Choke, 1 -> UnChoke, 2 -> Interested, 3 -> NotInterested)
	private val varMessages = Map[Int, Int=>WireMessage](
	4 -> finishHave, 5 -> finishBitfield, 6 -> this.finishRequest, 
	7 -> finishPiece, 8 -> finishCancel, 9 -> finishPort)
	
	def input(x:Byte) = queue.add(x)

	def nextMessage():WireMessage = {
		val length:Int = nextInt

		if(length == 0){ return KeepAlive }

		val id = nextByte

		if(id <= 3){ return constantMessages(id) }
		if(id <= 9){ return varMessages(id)(length - 1) }
		return finishUnknownMessage(id, length - 1)
	}

	private def finishHave(length:Int) = Have(nextInt)
	
	private def finishBitfield(length:Int):BitField = {
		val bytes = (1 to length).map( (x) => nextByte )
		var result = List[Boolean]()
		for(b <- bytes){
			for(i <- (7 to 0 by -1)){
				result = (if ((b & (0x1 << i)) > 0) true else false ) :: result
			}
		}
		
		return BitField(result.reverse)
	}

	private def finishRequest(length:Int) = Request(nextInt, nextInt, nextInt)
	
	private def finishPiece(length:Int):Piece = {
		val index = nextInt
		val begin = nextInt

		var build = new ByteStringBuilder
		(0 to (length - 8)).foreach { (x) => build += nextByte }

		return Piece(index, begin, build.result)
	}
	
	private def finishCancel(length:Int) = Cancel(nextInt, nextInt, nextInt)

	private def finishPort(length:Int) = Port(nextShort().toInt)

	private def finishUnknownMessage(id:Byte, length:Int):UnknownMessage = {
		var build = new ByteStringBuilder
		(1 to length).foreach { (x) => build += nextByte }

		return UnknownMessage(id, build.result)
	}

	private def nextByte():Byte = queue.take

	private def nextShort():Short = {
		val bytes = (1 to 2).map((x) => nextByte).map(x => x.toShort)
		((bytes(0) << 8) + bytes(1)).toShort
	}

	private def nextInt():Int = {
		val bytes = (1 to 4).map((x) => nextByte).map(x => x.toInt)
		(bytes(0) << 24) + (bytes(1) << 16) + (bytes(2) << 8) + bytes(3)
	}
}
