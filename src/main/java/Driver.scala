import java.math.BigInteger
import java.net.{URL, URLConnection}
import java.security.SecureRandom

import akka.actor.{ActorRef, Props, ActorSystem}
import org.apache.commons.io.IOUtils
import sun.net.www.content.text.PlainTextInputStream

object Driver {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("sbittorrent")
    //val fileChooser = new FileChooser(new File("input"))
    //fileChooser.showOpenDialog(null)
    //val filename = fileChooser.selectedFile
    //val src = Source.fromFile(filename)(ISO8859)
    //val metainfo = new Metainfo(src)
    //println(metainfo)
    //experimentingWithTracker(metainfo)
    //val client = system.actorOf(Props(new ClientActor(metainfo)), name="client")

    val client = system.actorOf(Props[FileManager],name="filemanager")
    val gui: ActorRef = system.actorOf(Props(new GUI(client)),name="gui")

    gui ! "start"
  }

  def experimentingWithTracker(metainfo: Metainfo): Unit = {
    val encodedInfoHash: String = URLUtil.toURLString(metainfo.infohash)
    val random = new SecureRandom
    val peer_id = new BigInteger(100, random).toString(32)
    val event = "started"
    val port = 6881
    val uploaded = "0"
    val downloaded = "0"
    val left = metainfo.fileLengths.values.sum
    val compact = "1"
    val trackerURL = metainfo.announce + "?event=" + event + "&info_hash=" + encodedInfoHash + "&peer_id=" + peer_id + "&port=" + port.toString + "&uploaded=" + uploaded + "&downloaded=" + downloaded + "&left=" + left + "&compact=" + compact + "&no_peer_id=0"
    contactTracker(new URL(trackerURL))
  }

  def contactTracker(url: URL): Unit = {
    println("Attempting to connect to: "+url.toString)
    val connect: URLConnection = url.openConnection
    val response = connect.getContent
    response match {
      case text: PlainTextInputStream => {
        val content = IOUtils.toString(text, "UTF-8")
        println(content)
        val parsed = Decode.readDict(content.tail)
        println(parsed)
      }
    }
  }

}
