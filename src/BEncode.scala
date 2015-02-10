import scala.io.Source

/**
 * Created by adam on 1/28/15.
 */
object BEncode extends (Source => MetaInfo) {
  override def apply(v1: Source): MetaInfo = {
    new MetaInfo()
  }
}


class MetaInfo {

}