package bittorrent.data

import java.security.MessageDigest
import java.util.Date
import scala.collection.mutable.MutableList

import bittorrent.parser._

import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.io.Source
import scala.collection.mutable.IndexedSeq

import akka.util.ByteString

import bittorrent.parser._

import akka.util.ByteString

object Metainfo {
	class File(val name:String,val length:Long,val md5sum:Seq[Byte] = null)
}

class Metainfo(source: Source) {
  import Metainfo._
  import Download._
  val bnodes : List[BNode] = Decode(source.mkString)
  var announce : String = null
  var comment : String = null
  var createdBy : String = null
  var creationDate : Date = null
  var announceList : MutableList[String] = MutableList.empty[String]
  private var filesM : MutableList[File] = new MutableList
  var fileLengths:mutable.Map[String, Int] = new mutable.LinkedHashMap[String, Int]
  var pieceLength:Int = -1
  var privateFlag:Int = -1
  private var pieceHashes : Seq[Byte] = null
  var infodic : String = null
  var name:String = null
  private var nameOpt: Option[String] = None
  private var lengthOpt: Option[Long] = None
  
  bnodes.head match {
    case dnode: DictNode => {
      for ((k: String,v) <- dnode.value) {
        v match {
          case vstring:StringNode => {
            k match {
              case "announce" => {
                announce = vstring.value
              }
              case "comment" => {
                comment = vstring.value
              }
              case "created by" => {
                createdBy = vstring.value
              }
              case _ => { }
            }
          }
          case vint:IntNode => {
            k match {
              case "creation date" => {
                creationDate = new Date(vint.value)
              }
              case _ => { }
            }
          }
          case vlist:ListNode => {
            k match {
              case "announce-list" => {
                for (e <- vlist.value) {
                  e match {
                    case lnode: ListNode => {
                      lnode.value.head match {
                        case announcer: StringNode => {
                          announceList += announcer.value
                        }
                      }
                    }
                    case _ => { }
                  }
                }
              }
              case _ => { }
            }
          }
          case vDic:DictNode => {
            k match {
              case "info" => {
                infodic = vDic.encoded
                for ((key,value) <- vDic.value) {
                  value match {
                    case lNode: ListNode => {
                      for(e <- lNode.value) {
                        e match {
                          case dic: DictNode => {
                            (dic.value.values.head,dic.value.values.tail.head) match {
                              case (ls : ListNode, len: IntNode) => {
                                ls.value.head match {
                                  case path: StringNode => {
                                    filesM += new File(path.value, len.value)
                                    fileLengths += ((path.value, len.value))
                                  }
                                }
                              }
                              case (len: IntNode, ls : ListNode) => {
                                ls.value.head match {
                                  case path: StringNode => {
                                    filesM += new File(path.value, len.value)
                                    fileLengths += ((path.value, len.value))
                                  }
                                }
                              }
                              case _ => {
                                println("Something else weird happened. Whatever.")
                              }
                            }
                          }
                          case _ => {
                            println("something weird happened, carry on")
                          }
                        }
                      }
                    }
                    case iNode: IntNode => {
                      key match {
                        case "piece length" => {
                          pieceLength = iNode.value
                        }
                        case "private" => {
                          privateFlag = iNode.value
                        }
                        case "length" => {
                          lengthOpt = Option(iNode.value)
                          fileLengths += ((null,iNode.value))
                        }
                        case _ => {
                          println(key)
                          println(iNode)
                        }
                      }
                    }
                    case s2Node: StringNode => {
                      key match {
                        case "name" => {
                          nameOpt = Option(s2Node.value)
                          name = s2Node.value
                        }
                        case "pieces" => {
                          pieceHashes = s2Node.value.getBytes("ISO-8859-1")
                        }
                        case _ => {}
                      }
                    }
                  }
                }
              }
            }
          }
          case _ => { }
        }
      }
    }
    case _ => { }
  }
  private val bytes = MessageDigest.getInstance("SHA-1").digest(infodic.getBytes("ISO-8859-1"))
  val infohash : ByteString = ByteString(bytes)
  val encodedInfohash = URLUtil.toURLString(bytes)

  /*  This throws a no such element exception and it doesn't appear to be used anywhere
  if(nameOpt.isDefined){
  	filesM += new File(nameOpt.get,lengthOpt.get)
  }
  */

  val totalLength: Long = filesM.foldLeft(0L){
	  (len:Long,file:File) => len + file.length
  }
  if (fileLengths.contains(null)) {
    fileLengths.put(name,fileLengths.get(null).get)
    fileLengths.remove(null)
  }

  val files : Seq[File] = filesM.toList
 
  val pieces: Seq[Piece] = (0 until pieceHashes.length/20).map {
	  i => new Piece(i,pieceLength,pieceHashes.slice(20*i,20*(i+1)))(this)
  }
  val total_pieces = pieces.length

  override def toString: String = {
    return "Announcde: "+announce+
    "\nComment: "+comment+
    "\nAnnounce List: "+announceList+
    "\nCreation Date: "+creationDate+
    "\nCreated By: "+createdBy+
    "\nPiece Length: " + pieceLength+
    "\nPrivate Flag: " + privateFlag+
    "\nfiles: " + files.mkString +
    "\nInfohash " + infohash
  }

  val fileLength = if (fileLengths.contains(null)) fileLengths.get(null).get else fileLengths.values.sum

}
