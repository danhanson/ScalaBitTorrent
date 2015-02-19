package bittorrent.peer

import akka.actor.{Actor,ActorRef,Extension,ExtensionId,ExtensionKey,ActorSystem,ExtendedActorSystem,ExtensionIdProvider}
import akka.io.{IO,Tcp}
import akka.io.Tcp.Command
import akka.io.Inet.SocketOption

import java.net.InetSocketAddress

object PeerWireProtocol extends ExtensionId[PeerWireExtension] with ExtensionIdProvider {

	override def lookup = this

	override def createExtension(system: ExtendedActorSystem): PeerWireExtension = new PeerWireExtension(system)

	object Manager {
		case class Bind(listener:ActorRef,endpoint:InetSocketAddress,backlog:Int,options:Traversable[SocketOption]=Nil, pullMode:Boolean=false) extends Command with Product with Serializable {
			
		}
	}

	class Manager extends Actor {
		import Manager._
		override def receive = {
			case Bind(listener,endpoint,backlog,options,pullmode) => println("sdfsdf")
				

			case _ => println("sdfSDF")
		}
	} 

}

class PeerWireExtension(system: ExtendedActorSystem) extends Extension {
	val tcp = Tcp.createExtension(system)
}
