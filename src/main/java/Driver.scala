import java.io.File
import java.security.MessageDigest

import scala.io.Codec.ISO8859
import scala.io.Source
import scala.swing.FileChooser
import scala.util.Random

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
    //println("Infodic " + metainfo.infodic)
    println("Infohash " + metainfo.infohash)

    var encodedInfoHash = java.net.URLEncoder.encode(metainfo.infohash)
    println(encodedInfoHash)
    var peer_id = new Array[Byte](20)
    println(peer_id.mkString)
    new Random().nextBytes(peer_id)
    println(peer_id.mkString)
    var encoded_peer_id = MessageDigest.getInstance("SHA-1").digest(peer_id).toString
    encoded_peer_id = java.net.URLEncoder.encode(encoded_peer_id)
    println("Peer_id: "+encoded_peer_id)
    var port = 8080
    var uploaded = "0"
    var downloaded = "0"
    var left = metainfo.fileLengths.values.sum
    var compact = "0"
    var trackerURL = metainfo.announce+"?info_hash="+encodedInfoHash+"&peer_id="+peer_id+"&port="+port.toString+"&uploaded="+uploaded+"&downloaded="+downloaded+"&left="+left+"&compact="+compact
    println(trackerURL)
    
    //val result = Decode(chars)
    //println(result)
  }
}
