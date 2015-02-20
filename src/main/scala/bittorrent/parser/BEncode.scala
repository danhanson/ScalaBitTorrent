package bittorent.parser

import scala.io.Source
import bittorrent.data.Metainfo
/*
/**
 * Created by adam on 1/28/15.
 */
object BEncode extends (Source => Metainfo) {
  override def apply(v1: Source): Metainfo = {
    new Metainfo(v1)
  }
}
*/