package bittorrent.peer

object Message {
	final case class HandShake(val pstrlen:Byte,val pstr:String,val reserved:Long,val infoHash:String, val peerID:String) extends NoID(49 + pstrlen)
	final case class KeepAlive() extends NoID(0)
	final case class Choke() extends HasID(1,0)
	final case class Unchoke() extends HasID(1,1)
	final case class Interested() extends HasID(1,2)
	final case class NotIntrested() extends HasID(1,3)
	final case class Have(val index:Byte) extends HasID(5,4)
	final case class BitField(val bitFieldLength:Byte, val bitfield: Array[Byte]) extends HasID(1+bitFieldLength,5)
	final case class Request(val index: Int, val begin: Int, val RequestLength: Int) extends HasID(13,6);
}


abstract sealed class Message(val length:Int){
	def hasID : Boolean
	def getID : Integer
}

abstract sealed class NoID(length: Int) extends Message(length){
	final override def hasID : Boolean = false;
	final override def getID : Nothing = {
		throw new NoSuchElementException()
	}
}

abstract sealed class HasID(length:Int, id:Byte) extends Message(length){
	final override def hasID : Boolean = true
	final override def getID : Integer = id
}
