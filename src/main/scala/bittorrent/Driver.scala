package bittorrent

import java.math.BigInteger
import java.net.{URL, URLConnection}
import java.security.SecureRandom

import akka.actor.{ActorRef, ActorSystem, Props}
import bittorrent.client.{FileManager, GUI}
import bittorrent.metainfo.{URLUtil, Metainfo}
import bittorrent.parser.Decode
import org.apache.commons.io.IOUtils


object Driver {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("sbittorrent")
    val client = system.actorOf(Props[FileManager],name="filemanager")
    val gui: ActorRef = system.actorOf(Props(new GUI(client)),name="gui")

    gui ! "start"
  }

}
