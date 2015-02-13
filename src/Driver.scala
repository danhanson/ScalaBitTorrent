import scala.io.Source
import scala.io.Codec.ISO8859

/**
 * Created by michaea1 on 2/13/2015.
 */
object Driver {
  def main(args: Array[String]): Unit = {
    val filename = "input/torrent.torrent"
    val chars = Source.fromFile(filename)(ISO8859).mkString
    val result = Decode(chars)
    println(result)
  }
}
