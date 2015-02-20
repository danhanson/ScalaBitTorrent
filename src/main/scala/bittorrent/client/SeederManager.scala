package bittorrent.client

import akka.actor.{ActorRef,Actor}
import bittorrent.data.Download

class SeederManager(seeder: ActorRef)(implicit download: Download, torrent: Torrent) extends Actor {

	def receive = {
		case _ => println("seeder")
	}
}