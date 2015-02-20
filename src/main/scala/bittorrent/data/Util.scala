package bittorrent.data

object URLUtil {
  def toURLString(in:Seq[Byte]): String = {
    var ch: Byte = 0x00
    var i = 0
    if (in == null || in.length <= 0) return null
    val pseudo: Array[String] = Array[String]("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
    val out = new StringBuffer(in.length * 2)
    while (i < in.length) {
      if ((in(i) >= '0' && in(i) <= 9)||(in(i)>='a'&&in(i)<='z')||(in(i)>='A'&&in(i)<='Z')||in(i)=='$'||in(i)=='-'||in(i)=='_'||in(i)=='.'||in(i)=='+'||in(i)=='!') {
        out.append(in(i).asInstanceOf[Char])
      } else {
        out.append('%')
        ch = (in(i)&0xF0).asInstanceOf[Byte]
        ch = (ch >>> 4).asInstanceOf[Byte]
        ch = (ch & 0x0F).asInstanceOf[Byte]
        out.append(pseudo(ch.asInstanceOf[Int]))
        ch = (in(i)&0x0F).asInstanceOf[Byte]
        out.append(pseudo(ch.asInstanceOf[Byte]))
      }
      i = i+1
    }
    return new String(out)
  }
}
