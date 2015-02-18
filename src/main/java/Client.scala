
/*
object Client {
	implicit val system: ActorSystem = ActorSystem()
	val internet : ActorRef = IO(Http)
	val clientID = "-3d0000-"
}

class Client(val port: Int = 6881) extends Actor {

	import Client._

	val files = new HashMap[String,TorrentFileHandler]

	val peerID = clientID + (0 to 12 map (x => Random.nextInt(10))).mkString

	override def receive = {
		case res: HttpResponse => 
		case _ => println("receive")
	}

	def torrent(meta: Metainfo): Unit = {
		val handler = new TorrentFileHandler(meta)(this)
		//files.put(meta.info.name,handler)
	}
}
*/