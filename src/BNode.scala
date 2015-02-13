/**
 * Created by michaea1 on 2/13/2015.
 */
trait BNode

class StringNode(value:String) extends BNode {
  override def toString: String = {
    "StringNode["+value+"]"
  }
}

class ListNode(value:List[BNode]) extends BNode {
  override def toString(): String = {
    "ListNode["+value.toString+"]"
  }
}

class DictNode(value:Map[BNode,BNode]) extends BNode {
  override def toString(): String = {
    "DictNode["+value.toString+"]"
  }
}

class IntNode(value:Int) extends BNode {
  override def toString: String = {
    "IntNode["+value+"]"
  }
}