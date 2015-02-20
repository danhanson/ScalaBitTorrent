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

import bittorrent.data.Download.{BitField => BitFieldData,Block,Piece}
import bittorrent.data.HasPieces
import bittorrent.client.Torrent

import scala.collection.immutable.Traversable

import java.net.InetSocketAddress

object Pwp extends ExtensionId[PwpExtension] with ExtensionIdProvider {

	override def lookup = this

	override def createExtension(system: ExtendedActorSystem): PwpExtension = new PwpExtension(system)

		abstract sealed class Message extends Product with Serializable 

		abstract sealed class Command extends Message
	
		abstract sealed class HasLength(val length:Int) extends Message {
			def hasId : Boolean
			def getId : Integer
			def tcpMessage : Tcp.Message = Tcp.Close
			protected def messageBytes: ByteString = ByteString()
		}

		abstract sealed class HasId(length:Int, id:Byte) extends HasLength(length){
			final override def hasId : Boolean = true
			final override def getId : Integer = id
		}

		abstract sealed class NoId(length: Int) extends HasLength(length){
			final override def hasId : Boolean = false;
			final override def getId : Nothing = {
				throw new NoSuchElementException()
			}
		}

		case class Bind(
				val listener:ActorRef,
				val endpoint:InetSocketAddress,
				val backlog:Int = 100,
				val options:Traversable[SocketOption]=Nil,
				val pullMode:Boolean=false
			) extends Command

		case class HandShake(
				val protocol: String,
				val peer: Peer
			) extends NoId(49 + protocol.length())

		case class KeepAlive()    extends NoId(0)

		case class Choke()        extends HasId(1,0)

		case class Unchoke()      extends HasId(1,1)

		case class Interested()   extends HasId(1,2)

		case class NotIntrested() extends HasId(1,3)

		case class Have(val index:Byte) extends HasId(5,4)

		object BitField{
			def apply(hasPieces: HasPieces): BitField = {
				BitField(new BitFieldData(hasPieces))
			}
		}

		case class BitField(
				val value: BitFieldData
			) extends HasId(1+value.bytes.length,5)

		case class Request(
				val block: Block
			) extends HasId(13,6);

		case class Piece(
				val block: Block,
				val data: ByteString
			) extends HasId(9+block.length,7)

		case class Cancel() extends HasId(13,8)

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
