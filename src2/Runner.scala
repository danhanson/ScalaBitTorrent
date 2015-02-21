package peer

object Main {
	def main(args: Array[String]){
		println(KeepAlive.encode)
		println(Choke.encode)
		println(UnChoke.encode)
		println(Have(17).encode)
		println(Request(1, 2, 3).encode)
		println(BitField(List(true, false, true)).encode)
		println(BitField(List(true, false, false, true, false, false, true, false, false)).encode)
		println(Port(11).encode)
		println(Port(65536).encode)
		println(Port(65535).encode)

		val parse = new MessageParser
		
		for(x <- Request(1, 2, 3).encode) { parse.input(x) }
		for(x <- KeepAlive.encode) { parse.input(x) }
		for(x <- Port(11).encode) { parse.input(x) }
		for(x <- BitField(List(true, false, false, true, false, false, true, false, false)).encode) { parse.input(x) }

		for(i <- 1 to 4) { println(parse.nextMessage) }
	}
}
