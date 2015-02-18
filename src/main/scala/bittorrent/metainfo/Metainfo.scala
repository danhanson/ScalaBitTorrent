package bittorrent.metainfo

import java.util.Date

import scala.collection.mutable.MutableList
import scala.collection.mutable
import scala.io.Source

import java.security.MessageDigest
import bittorrent.parser._

object Metainfo {
	
	class File {
		val pieceLength : Int
		val pieces : Seq[String]
		val isPrivate : Boolean
	}

	object FileMode {
		case object SingleFileMode extends FileMode	
		case object MultipleFilesMode extends FileMode
	}

	sealed abstract trait FileMode

	object Info {
		class SingleFileInfo extends File with Info {
			override val fileMode = FileMode.SingleFileMode
			val name: String
			val length: Long
			val md5sum: String
		}

		class MultipleFilesInfo extends Info {
			override val fileMode = FileMode.MultipleFilesMode
			val name: String
			val files: Seq[File]
		}
	}

	sealed trait Info {
		val pieceLength: Long
		val pieces: Seq[String]
		val isPrivate: Boolean
		val files: Seq[File]
		val fileMode: FileMode
	}

	val sha1Encoder = MessageDigest.getInstance("SHA-1")

	private def makeInfoHash(string: String): String = {
		sha1Encoder.digest(string.getBytes("UTF-8")).toString()
	}

	private def stringOrEmpty(option: Option[BNode]): String = {
	  if(option.isEmpty)
	    ""
	  else
	    option.get.value.asInstanceOf[String]
	}

	private def dateOrNull(option: Option[BNode]): Date = {
	  if(option.isEmpty)
	 	  null
	  else
	 	  new Date(option.get.value.asInstanceOf[Int])
	}
	
}

class Metainfo(source: Source) {
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
  var pieces : String = null

  bnodes.head match {
    case dnode: DictNode => {
      for ((k,v) <- dnode.value) {
        (k,v) match {
          case (kstring:String,vstring:StringNode) => {
            kstring match {
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
          case (kstring:StringNode,vint:IntNode) => {
            kstring.value match {
              case "creation date" => {
                creationDate = new Date(vint.value)
              }
              case _ => { }
            }
          }
          case (kstring:StringNode,vlist:ListNode) => {
            kstring.value match {
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
          case (kstring:StringNode, vDic:DictNode) => {
            kstring.value match {
              case "info" => {
                for ((key,value) <- vDic.value) {
                  (key, value) match {
                    case (sNode: StringNode, lNode: ListNode) => {
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
                            }
                          }
                        }
                      }
                    }
                    case (sNode: StringNode, iNode: IntNode) => {
                      sNode.value match {
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
                          println(sNode)
                          println(iNode)
                        }
                      }
                    }
                    case (sNode: StringNode, s2Node: StringNode) => {
                      sNode.value match {
                        case "name" => {
                          name = s2Node.value
                        }
                        case "pieces" => {
                          pieces = s2Node.value
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


}
