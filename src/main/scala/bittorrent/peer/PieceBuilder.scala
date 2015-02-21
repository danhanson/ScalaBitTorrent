package bittorrent.peer

import akka.util.ByteString
import bittorrent.data.Metainfo
import scala.collection.mutable.{ListBuffer, Buffer}

/**
 * This may be an awful idea, but I thought it was kind of "scala-ish"
 * Since pieces are broken between messages, we instantiate an instance
 * of this function and call it repeatedly until the entire piece is consumed.
 */
class PieceBuilder(val metainfo: Metainfo,val first_message:ByteString) extends (ByteString => (Int,Int,Array[Byte])) {
  var lengthPrefix: Int = first_message.take(4).toByteBuffer.getInt
  var messageId: Byte = first_message.drop(4).head
  var index: Int = first_message.drop(5).take(4).asByteBuffer.getInt
  var offset: Int = first_message.drop(9).take(4).asByteBuffer.getInt
  var current_block: Buffer[Byte] = first_message.drop(13).toBuffer
  var total_piece: Buffer[Byte] = ListBuffer.empty[Byte]
  var remaining_in_block = lengthPrefix - 9 - current_block.length
  var inside_of_block = true

  override def apply(msg: ByteString): (Int, Int, Array[Byte]) = {
    println("Calling the function")
    println(inside_of_block)
    println(remaining_in_block)
    println(total_piece)
    println(index)
    if (inside_of_block) {
      current_block ++= msg
      //println("Status (piece "+index+"): "+piece.length+" / "+(lengthPrefix-9)+" bytes")
      if (current_block.length + 9 == lengthPrefix) {
        total_piece ++= current_block
        //if (total_piece.length == piece_length) {
        if (total_piece.length == metainfo.pieceLength || (index == metainfo.total_pieces-1 && index*16384+total_piece.length == metainfo.fileLength)) {
          return (index, -1, total_piece.toArray)
        }
        current_block = ListBuffer.empty[Byte]
        inside_of_block = false
        return (index, offset + 16384, null)
      }
      return null
    } else {
      lengthPrefix = msg.take(4).toByteBuffer.getInt
      messageId = msg.drop(4).head
      index = msg.drop(5).take(4).asByteBuffer.getInt
      offset = msg.drop(9).take(4).asByteBuffer.getInt
      current_block ++= msg.drop(13)
      remaining_in_block = lengthPrefix - 9 - current_block.length
      inside_of_block = true
      return null
    }
  }
}
