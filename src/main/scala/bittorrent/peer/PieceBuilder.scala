package bittorrent.peer

import akka.util.ByteString
import scala.collection.mutable.Buffer

/**
 * This may be an awful idea, but I thought it was kind of "scala-ish"
 * Since pieces are broken between messages, we instantiate an instance
 * of this function and call it repeatedly until the entire piece is consumed.
 */
class PieceBuilder(val first_message:ByteString) extends (ByteString => (Int,Array[Byte])) {
  val lengthPrefix: Int = first_message.take(4).toByteBuffer.getInt
  val messageId: Byte = first_message.drop(4).head
  val index: Int = first_message.drop(5).take(4).asByteBuffer.getInt
  val offset: Int = first_message.drop(9).take(4).asByteBuffer.getInt
  val piece: Buffer[Byte] = first_message.drop(13).toBuffer
  var remaining = lengthPrefix - 9 - piece.length

  override def apply(block: ByteString): (Int,Array[Byte]) = {
    piece ++= block
    //println("Status (piece "+index+"): "+piece.length+" / "+(lengthPrefix-9)+" bytes")
    if (piece.length+9 >= lengthPrefix) (index,piece.take(lengthPrefix-9).toArray)
    else null
  }
}
