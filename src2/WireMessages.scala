package peer

import akka.util.{ByteString, ByteStringBuilder}

abstract class WireMessage(id:Byte){
	def encode():ByteString = {
		val contentBuilder = new ByteStringBuilder
		
		// Message type
		if(id >= 0){
			contentBuilder += id;
		}

		// Message payload
		encodeContent().foreach {contentBuilder.append}

		// Message content
		val content = contentBuilder.result

		// Message length
		val length = encodeInt(content.length)

		val resultBuilder = new ByteStringBuilder
		resultBuilder.append(length)
		resultBuilder.append(content)

		return resultBuilder.result
	}

	def encodeInt(x: Int):ByteString = {
		val r = new ByteStringBuilder
		var in = x
		for( i <- 1 to 4 ){
			r += ((in & 0xff000000) >> 24).asInstanceOf[Byte]
			in = in << 8
		}

		return r.result;
	}

	def encodeContent():Seq[ByteString] = return List((new ByteStringBuilder).result)
}

case object KeepAlive extends WireMessage(-1)
case object Choke extends WireMessage(0)
case object UnChoke extends WireMessage(1)
case object Interested extends WireMessage(2)
case object NotInterested extends WireMessage(3)

case class Have(index:Int) extends WireMessage(4){
	override def encodeContent() = List(encodeInt(index));
}

case class BitField(bitfield:Seq[Boolean]) extends WireMessage(5){
	override def encodeContent() = {
		var bit:Int = 0
		var byte:Byte = 0

		val r = new ByteStringBuilder

		for( x <- bitfield){
			byte = (byte | (if(x) 0x1 else 0x0)).asInstanceOf[Byte]

			bit += 1

			if(bit == 8){
				r += byte
				byte = 0
				bit = 0
			}else{
				byte = (byte << 1).asInstanceOf[Byte]
			}
		}

		if( bit != 0 ){ 
			// If we weren't about to start a new byte anyway, then we need to
			// fill the last byte with 0s.
			byte = (byte << (8 - bit - 1)).asInstanceOf[Byte]
			r += byte
		}

		List(r.result)
	}
}

case class Request(index:Int, begin:Int, length:Int) extends WireMessage(6){
	override def encodeContent() = List(index, begin, length).map(encodeInt)
}

case class Piece(index:Int, begin:Int, block:ByteString) extends WireMessage(7){
	override def encodeContent() = List(encodeInt(index), encodeInt(begin), block)
}

case class Cancel(index:Int, begin:Int, length:Int) extends WireMessage(8){
	override def encodeContent() = List(index, begin, length).map(encodeInt)
}

case class Port(port:Int) extends WireMessage(9){
	override def encodeContent() = List(encodeInt(port).slice(2, 4))
}

case class UnknownMessage(id:Byte, content:ByteString) extends WireMessage(id){
	override def encodeContent() = List(content)
}


