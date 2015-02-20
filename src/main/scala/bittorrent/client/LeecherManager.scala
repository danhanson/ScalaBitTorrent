package bittorrent.client

import akka.actor.{Actor,ActorRef}
import akka.pattern.ask
import akka.actor.ReceiveTimeout
import scala.concurrent.Future
import bittorrent.pwp.Pwp
import bittorrent.data.Download
import akka.actor.Actor.Receive
import java.net.SocketAddress
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout

class LeecherManager(leecher: ActorRef)(implicit download: Download, client: Client) extends Actor {
	import Pwp._
	import context._

	setReceiveTimeout(2 minutes)
	private var choked : Boolean = true
	private var interested : Boolean = false
	
	leecher ! Bind(self,client.socketAddress)
	if(download.downloaded > 0){
		leecher ! Pwp.BitField(download)
	}

	override def receive = {
		case KeepAlive => 
			setReceiveTimeout(2 minutes)
		case Interested =>
			interested = true
			// TODO: check if capable of uploading data before unchoking
			leecher ! Unchoke()
		case r: Request =>
			val block = r.block
			if(download.hasBlock(block)){
				leecher ! Piece(block,download.getBlock(block))
			}
	}
}