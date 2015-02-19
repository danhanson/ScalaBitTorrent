package bittorrent.client

import akka.actor.ActorRef
import akka.actor.Actor
import bittorrent.pwp._
import bittorrent.pwp.Pwp._

object Seeder {
	
}

class Seeder extends Actor {
	
	override def receive = {
		case hs: HandShake => {
			println("impolitely ignored handshake")
		}

		case _ => {
			throw new Exception("THATS NO HANDSHAKE!")
		}
	}
}