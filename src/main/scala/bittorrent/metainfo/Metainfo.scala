package bittorrent.metainfo

import java.security.MessageDigest
import java.util.Date

import bittorrent.parser._

import scala.collection.mutable
import scala.collection.mutable.MutableList
import scala.io.Source

class Metainfo(val source: Source) {
  val bnodes : List[BNode] = Decode(source.mkString)
  var announce : String = null
  var comment : String = null
  var createdBy : String = null
  var creationDate : Date = null
  var announceList : MutableList[String] = MutableList.empty[String]
  var fileLengths : mutable.HashMap[String, Int] = new mutable.HashMap[String, Int]
  var pieceLength : Int = -1
  var privateFlag : Int = -1
  var name : String = null
  var piecesArray : Array[Byte] = null
  var infodic : String = null

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
                                    fileLengths += ((path.value, len.value))
                                  }
                                }
                              }
                              case (len: IntNode, ls : ListNode) => {
                                ls.value.head match {
                                  case path: StringNode => {
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
                          name = s2Node.value
                        }
                        case "pieces" => {
                          piecesArray = s2Node.value.getBytes("ISO-8859-1")
                        }
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

  val infohash: Array[Byte] = MessageDigest.getInstance("SHA-1").digest(infodic.getBytes("ISO-8859-1"))
  val pieces: Array[Array[Byte]] = new Array[Array[Byte]](piecesArray.length/20)
  for (i <- 0 to piecesArray.length / 20 - 1) {
    pieces(i) = piecesArray.slice(20*i,20*(i+1))
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
    "\nName: " + name+
    "\nFile Lengths: " + fileLengths+
    "\nInfohash " + infohash
  }
}
