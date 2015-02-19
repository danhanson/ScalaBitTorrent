package bittorrent.client

import akka.actor.ActorRef
import akka.actor.Actor
import bittorrent.peer.Message._

object Seeder {
	
}

class Seeder extends Actor {


	
	override def receive = {
		case hs: HandShake => {
			
		}

		case _ => {
			throw new Exception("THATS NO HANDSHAKE!")
		}
	}
}