import java.io.File
import java.net.{URLConnection, URL}
import java.security.MessageDigest

import akka.io.IO
import akka.remote.transport.netty.NettyTransportSettings.Udp
import spray.client.pipelining._
import spray.http.HttpRequest
import sun.net.www.content.text.PlainTextInputStream
import org.apache.commons.io.IOUtils

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
    var event="started"
    var port = 6881
    var uploaded = "0"
    var downloaded = "0"
    var left = metainfo.fileLengths.values.sum
    var compact = "1"
    var trackerURL = metainfo.announce+"?event="+event+"&info_hash="+encodedInfoHash+"&peer_id="+encoded_peer_id+"&port="+port.toString+"&uploaded="+uploaded+"&downloaded="+downloaded+"&left="+left+"&compact="+compact+"&no_peer_id=0"
    val conn: URLConnection = new URL(trackerURL.replace("udp","http")).openConnection
    val what: AnyRef = conn.getContent
    println(conn)
    println(what)
    what match {
      case text: PlainTextInputStream => {
        var content = IOUtils.toString(text,"UTF-8")
        println(content)
      }
    }
    var res: HttpRequest = Get(trackerURL)
    IO(Udp) ! Udp.SimpleSenderReady =>
      context.become(ready(sender()))

    //new Tracker(metainfo,trackerURL).sendRequest

    //implicit val system = ActorSystem("simple-spray-client")
    //val log = Logging(system, getClass)
    //log.info("Requesting stuff")
    //println(trackerURL)



    //val result = Decode(chars)
    //println(result)
  }
}
