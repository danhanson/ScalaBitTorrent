import scala.io.Source
import scala.io.Codec.ISO8859

/**
 * Created by michaea1 on 1/31/2015.
 */
object Driver {
  def main(args: Array[String]): Unit = {
    val filename = "input/hello"
    val chars = Source.fromFile(filename)(ISO8859)
    val par = new Parse()
    val result = chars.foldLeft(List.empty[String])(par)
    println(result)

  }

}
