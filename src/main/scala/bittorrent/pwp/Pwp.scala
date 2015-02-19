package bittorrent.pwp

import akka.actor.{Actor,ActorRef,ExtensionId,ExtensionKey,ActorSystem,ExtendedActorSystem,ExtensionIdProvider}
import akka.io.{IO,Tcp}
import akka.io.IO.Extension
import akka.io.Tcp.Command
import akka.io.{Tcp, TcpMessage}
import akka.io.Inet.SocketOption
import akka.actor.Props
import akka.util.ByteString
import spray.io.BackPressureHandling.Ack

import scala.collection.immutable.Traversable

import java.net.InetSocketAddress

object Pwp extends ExtensionId[PwpExtension] with ExtensionIdProvider {

	override def lookup = this

	override def createExtension(system: ExtendedActorSystem): PwpExtension = new PwpExtension(system)

		abstract sealed class Message extends Product with Serializable 

		abstract sealed class Command extends Message
	
		abstract sealed class HasLength(val length:Int) extends Message {
			def hasID : Boolean
			def getID : Integer
			def tcpMessage : Tcp.Message = Tcp.Close
			protected def messageBytes: ByteString = ByteString()
		}

		abstract sealed class HasID(length:Int, id:Byte) extends HasLength(length){
			final override def hasID : Boolean = true
			final override def getID : Integer = id
		}

		abstract sealed class NoID(length: Int) extends HasLength(length){
			final override def hasID : Boolean = false;
			final override def getID : Nothing = {
				throw new NoSuchElementException()
			}
		}

		case class Bind(
				listener:ActorRef,
				endpoint:InetSocketAddress,
				backlog:Int,
				options:Traversable[SocketOption]=Nil,
				pullMode:Boolean=false
			) extends Command

		case class HandShake(
				val pstrlen:Byte,
				val pstr:String,
				val reserved:Long,
				val infoHash:String,
				val peerID:String
			) extends NoID(49 + pstrlen)

		case class KeepAlive()    extends NoID(0)

		case class Choke()        extends HasID(1,0)

		case class Unchoke()      extends HasID(1,1)

		case class Interested()   extends HasID(1,2)

		case class NotIntrested() extends HasID(1,3)

		case class Have(val index:Byte) extends HasID(5,4)

		case class BitField(
				val bitFieldLength:Byte,
				val bitfield: Array[Byte]
			) extends HasID(1+bitFieldLength,5)

		case class Request(
				val index: Int,
				val begin: Int, 
				val RequestLength: Int
			) extends HasID(13,6);

	class PwpProxy(peer: ActorRef) extends Actor {
		override def receive = {
			case cmd: Tcp.Message => println("Message recieved"+cmd.toString())// TODO: parse peer wire protocol messages from tcp messages and send them to peer
		}
	}

	class Manager(tcp: ActorRef)extends Actor {

		override def receive = {
			case Bind(listener,endpoint,backlog,options,pullmode) => println("listening for connections")
				val p = context.system.actorOf(Props(new PwpProxy(listener)))
				tcp ! Tcp.Bind(p,endpoint,backlog,options,pullmode)

			case m: Message => println("MESSAGE SENT") // TODO: convert peer messages to tcp messages, consider adding toTcp method to message
			case _ => println("sdfSDF")
		}
	} 

}

class PwpExtension(system: ExtendedActorSystem) extends Extension {
	import Pwp._

	implicit val asystem : ActorSystem = system

	private val tcp = Tcp.createExtension(system)

	private lazy val managerVal = system.actorOf(Props(new Manager(tcp.getManager)))

	override def manager : ActorRef = managerVal
}
