package bittorrent.parser

import scala.collection.mutable

trait BNode { val value : Any }

class StringNode(val value:String) extends BNode {
  override def toString: String = {
    "StringNode["+value+"]"
  }
}

class ListNode(val value:List[BNode]) extends BNode {
  override def toString(): String = {
    "ListNode["+value.toString+"]"
  }
}

class DictNode(val value:Map[String,BNode],val encoded:String) extends BNode {
  override def toString(): String = {
    "DictNode["+value.toString+"]"
  }
}

class IntNode(val value:Int) extends BNode {
  override def toString: String = {
    "IntNode["+value+"]"
  }
}