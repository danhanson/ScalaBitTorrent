import java.util.Date

import scala.collection.mutable.MutableList
import scala.io.Source

class Metainfo(source: Source) {
  val bnodes : List[BNode] = Decode(source.mkString)
  var announce : String = null
  var comment : String = null
  var createdBy : String = null
  var creationDate : Date = null
  var announceList : MutableList[String] = MutableList.empty[String]

  bnodes.head match {
    case dnode: DictNode => {
      for ((k,v) <- dnode.value) {
        (k,v) match {
          case (kstring:StringNode,vstring:StringNode) => {
            kstring.value match {
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
          case _ => { }
        }
      }
    }
    case _ => { }
  }

  def getInfo() : String = "";

}
