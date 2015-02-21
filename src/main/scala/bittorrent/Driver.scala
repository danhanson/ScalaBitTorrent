package bittorrent

import akka.actor.{ActorRef, ActorSystem, Props}
import bittorrent.client.{FileManager, GUI}

object Driver {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("bittorrent")
    val client = system.actorOf(Props[FileManager],name="filemanager")
    val gui: ActorRef = system.actorOf(Props(new GUI(client)),name="gui")
  }
}
