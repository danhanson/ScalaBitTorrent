package bittorrent.pwp

import akka.actor.{Actor,ActorRef,ExtensionId,ExtensionKey,ActorSystem,ExtendedActorSystem,ExtensionIdProvider}
import akka.io.{IO,Tcp}
import akka.io.IO.Extension
import akka.io.Tcp.Command
import akka.util.Timeout
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

import scala.concurrent.Future
import akka.pattern.ask

object Pwp {
	implicit val system: ActorSystem = ActorSystem("bittorrent")

	val tcp: ActorRef = IO(Tcp)

	abstract sealed class Message extends Product with Serializable {
		def tcpMessage : Tcp.Message = {
			throw new Exception("tcpMessage not implemented")
		}
	}

	abstract sealed class Command extends Message

	abstract sealed class HasLength(val length:Int) extends Message {
		def hasId : Boolean
		def getId : Integer
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
		
	case class Close() extends Command
		case class HandShake(
			val protocol: ByteString,
			val infohash: ByteString,
			val peerId: ByteString
		) extends NoId(49 + protocol.size)

	case class KeepAlive()    extends NoId(0)
	case class Choke()        extends HasId(1,0)
	case class Unchoke()      extends HasId(1,1)
	case class Interested()   extends HasId(1,2)
	case class NotIntrested() extends HasId(1,3)
	case class Have(val index:Int) extends HasId(5,4)
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
}

class Pwp(val addr: InetSocketAddress) extends Actor {
	import Pwp._
	import scala.concurrent.ExecutionContext.Implicits.global

	implicit val timeout: Timeout = new Timeout(2000)

	override def receive = {
		case msg: Pwp.Message => (tcp ? msg.tcpMessage).mapTo[Tcp.Message].onComplete {
			x => println(x.get.toString)
		}
		case _ => throw new Exception("Not a valid PWP message")
	}
}
