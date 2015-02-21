package peer

import akka.actor.{Actor, Props}

class Peer extends Actor {

	var amChoking:Boolean = true
	var amInterested:Boolean = false

	var peerChoking:Boolean = true
	var peerInterested:Boolean = false

	def receive = {
		case x:Any => println(x);
	}

}
