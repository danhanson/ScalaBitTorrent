package bittorrent.client

import java.awt
import java.io.File

import akka.actor.{Actor, ActorRef}
import bittorrent.peer.PeerManagerUpdate
import bittorrent.tracker.TrackerStatusUpdate

import scala.collection.mutable
import scala.swing.GridBagPanel.Fill
import scala.swing._
import scala.swing.event.ButtonClicked
import scala.collection.parallel.mutable.ParHashMap

// the filemanager constructor is so the GUI knows who to send
// messages to when you select a new file
class GUI(filemanager:ActorRef) extends Actor {
  val displayed_torrents = new ParHashMap[Int,TorrentDisplay]
  var nextId = 0
  @volatile var table:Table = null
  @volatile var me = self
  var opened:Boolean = false

  frame.open

  def frame = new Frame {
    title = "sbittorrent"
    val button = new Button {
      text = "new download"
    }
    val headers: Seq[String] = Array("Name","Seeders","Leechers","Peers","Pieces")
    val rowData: Array[Array[Any]] = Array.tabulate[Any](25,5) ((_,_) => "")
    table = new Table(rowData, headers) {
      selection.elementMode = Table.ElementMode.Cell
    }
    val tablePane = new ScrollPane(table)
    contents = new GridBagPanel {
      val c = new Constraints
      val shouldFill = true
      c.fill = Fill.Horizontal
      c.weightx=0.5
      c.gridx = 0
      c.gridy = 0
      layout(button) = c
      c.gridy = 1
      layout(tablePane) = c
      border = Swing.EmptyBorder(30,30,10,30)
    }
    listenTo(button)
    reactions += {
      case ButtonClicked(b) =>
        val fileChooserOpen = new FileChooser(new File("input"))
        fileChooserOpen.showOpenDialog(null)
        val openFile: File = fileChooserOpen.selectedFile
        if(openFile != null) {
          val fileChooserSave = new FileChooser(new File("output"))
          fileChooserSave.showSaveDialog(null)
          val saveFile: File = fileChooserSave.selectedFile
          if (saveFile != null) {
            // closing the filechooser gives a null file
            displayed_torrents.put(nextId, new TorrentDisplay(openFile))
            filemanager ! (me, nextId, openFile, saveFile)
            nextId += 1
          }
        }
    }
  }

  // we will use this to send updates to the GUI
  // to show status of the torrents
  override def receive: Receive = {
    case update:TrackerStatusUpdate =>
      this.synchronized {
        //displayed_torrents.get(update.id).get.trackerUpdate(update)
        displayed_torrents.get(update.id).foreach(x=>x.trackerUpdate(update))
        refresh
      }
    case update:PeerManagerUpdate =>
      this.synchronized {
        //displayed_torrents.get(update.id).get.peerManagerUpdate(update)
        displayed_torrents.get(update.id).foreach(x=>x.peerManagerUpdate(update))
        refresh
      }
    case x =>
      println("SimpleSwingApplication received the message: " + x)
  }

  def refresh: Unit = {
    table.synchronized {
      for ((row,torrent) <- displayed_torrents) {
        table.update(row,0,torrent.name)
        table.update(row,1,torrent.complete)
        table.update(row,2,torrent.incomplete)
        table.update(row,3,torrent.peers)
        table.update(row,4,torrent.status)
      }
    }
  }

}

class TorrentDisplay(val file:File) {
  val name: String = file.getName
  var incomplete: String = "?"
  var complete:String = "?"
  var peers:String = "?"
  var status:String = "?/?"

  def trackerUpdate(update:TrackerStatusUpdate): Unit = {
    incomplete = update.incomplete.toString
    peers = update.peers.toString
    complete = update.complete.toString
  }

  def peerManagerUpdate(update:PeerManagerUpdate): Unit = {
    val ours = update.collected_pieces.toString
    val total = update.total_pieces.toString
    status = ours+"/"+total
  }
}
