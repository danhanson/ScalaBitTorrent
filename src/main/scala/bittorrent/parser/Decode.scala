package bittorrent.parser

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Decode extends (String => List[BNode]) {

  override def apply(v1: String): List[BNode] = {
    var remaining : String = v1
    val result = new ListBuffer[BNode]
    while (!remaining.isEmpty) {
      val output = readOne(remaining)
      result.append(output._1)
      remaining = output._2
    }
    result.toList
  }

  def readOne(input: String): (BNode,String) = {
    if (input.head.getNumericValue < 10)
      readString(input)
    else if (input.head.equals('i'))
      readInteger(input.tail)
    else if (input.head.equals('l'))
      readList(input.tail)
    else if (input.head.equals('d'))
      readDict(input.tail)
    else
      throw new Exception("Unable to decode a bencoded message")
  }

  def readString(input: String): (StringNode,String) = {
    val partitions = input.split(":")
    val len = partitions.head.toInt
    val tuple = partitions.tail.mkString(":").splitAt(len)
    (new StringNode(tuple._1),tuple._2)
  }

  def readList(input: String): (ListNode,String) = {
    var remaining = input
    val result = new ListBuffer[BNode]
    while (!remaining.head.equals('e')) {
      val output = readOne(remaining)
      result.append(output._1)
      remaining = output._2
    }
    (new ListNode(result.toList), remaining.tail)
  }

  def readInteger(input: String): (IntNode,String) = {
    val partitions = input.splitAt(input.indexOf("e"))
    val value = partitions._1.toInt
    val tail = partitions._2.tail
    (new IntNode(value), tail)
  }

  def readDict(input: String): (DictNode,String) = {
    var remaining = input
    val result = new mutable.HashMap[String,BNode]
    while (!remaining.head.equals('e')) {
      val output1 = readOne(remaining)
      val output2 = readOne(output1._2)
      result.put(output1._1.asInstanceOf[StringNode].value,output2._1)
      remaining = output2._2
    }
    (new DictNode(result.toMap,'d'+input.substring(0,input.length-remaining.length)+'e'), remaining.tail)
  }

  def asDictionary(v1: String): Map[String,BNode] = {
    if(v1.head != 'd') throw new IllegalArgumentException()
      readDict(v1.tail)._1.value.toMap
  }
}
