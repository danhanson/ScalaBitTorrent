package bittorrent.metainfo

import scala.io.Source

// if we ever figure out udp we can get rid of this
class HTTPOnlyMetainfo(val src:Source) extends Metainfo(src) {
  if (!announce.startsWith("http")) {
    for (alt <- announceList) {
      if (alt.startsWith("http") && !alt.contains("ipv6")) {
        announce = alt
      }
    }
  }
}
