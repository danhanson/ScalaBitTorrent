import scala.io.Source
import scala.io.Codec.ISO8859

/**
 * Created by michaea1 on 2/13/2015.
 */
object Driver {
  def main(args: Array[String]): Unit = {
    val filename = "input/torrent.torrent"
    val chars = Source.fromFile(filename)(ISO8859).mkString
    val src = Source.fromFile(filename)(ISO8859)
    val metainfo = new Metainfo(src)
    println(metainfo.announce)
    println(metainfo.comment)
    println(metainfo.announceList)
    println(metainfo.creationDate)
    println(metainfo.createdBy)
    //val result = Decode(chars)
    //println(result)
  }
}
