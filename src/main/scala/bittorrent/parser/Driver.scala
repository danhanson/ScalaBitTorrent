package bittorrent.parser

import scala.io.Source
import scala.io.Codec.ISO8859
import scala.swing.FileChooser
import java.io.File
import bittorrent.metainfo._
import bittorrent.client.Client

/**
 * Created by michaea1 on 2/13/2015.
 */
object Driver {
  def main(args: Array[String]): Unit = {
    val fileChooser = new FileChooser(new File("input"))
    fileChooser.showOpenDialog(null)
    val filename = fileChooser.selectedFile
    val chars = Source.fromFile(filename)(ISO8859).mkString
    val src = Source.fromFile(filename)(ISO8859)
    val metainfo = new Metainfo(src)
    println(metainfo.announce)
    println(metainfo.comment)
    println(metainfo.announceList)
    println(metainfo.creationDate)
    println(metainfo.createdBy)
    println("Piece Length: " + metainfo.pieceLength)
    println("Private Flag: " + metainfo.privateFlag)
    println("Name: " + metainfo.name)
    println("File Lengths: " + metainfo.fileLengths)
    val client = new Client()
    if(metainfo != null){
    	client.torrent(metainfo);
    }
  }
}
