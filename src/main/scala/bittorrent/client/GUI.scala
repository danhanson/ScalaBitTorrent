package bittorrent.client

import java.awt
import java.io.File
import javax.swing.JFrame

import akka.actor.{Actor, ActorRef}
import bittorrent.peer.{ActivePeersUpdate, PeerManagerUpdate}
import bittorrent.tracker.TrackerStatusUpdate

import scala.collection.mutable
import scala.swing.FileChooser.SelectionMode
import scala.swing.FileChooser.SelectionMode._
import scala.swing.GridBagPanel.Fill
import scala.swing._
import scala.swing.event.ButtonClicked
import scala.collection.parallel.mutable.ParHashMap

class GUIUpdate(var id:Int)

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
    peer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val button = new Button {
      text = "new download"
    }
    val headers: Seq[String] = Array("Name","Seeders","Leechers","Peers","Pieces","Speed")
    val rowData: Array[Array[Any]] = Array.tabulate[Any](25,6) ((_,_) => "")
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
          fileChooserSave.fileSelectionMode = DirectoriesOnly
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
        displayed_torrents.get(update.id).foreach(x=>x.trackerUpdate(update))
        refresh
      }
    case update:PeerManagerUpdate =>
      this.synchronized {
        displayed_torrents.get(update.id).foreach(x=>x.peerManagerUpdate(update))
        refresh
      }
    case update:ActivePeersUpdate =>
      this.synchronized {
        displayed_torrents.get(update.id).foreach(x=>x.activePeersUpdate(update))
        refresh
      }
    case x =>
      println("SimpleSwingApplication received the message: " + x)
  }

  def refresh: Unit = {
    table.synchronized {
      for ((row,torrent) <- displayed_torrents) {
        val complete:String = if (torrent.complete == -1) "?" else torrent.complete.toString
        val incomplete:String = if (torrent.incomplete == -1) "?" else torrent.incomplete.toString
        val peers:String = if (torrent.peers == -1) "?" else torrent.active+"/"+torrent.peers
        val status:String = if (torrent.downloaded_pieces == -1) "?/?" else Math.min(torrent.downloaded_pieces,torrent.total_pieces)+"/"+torrent.total_pieces
        val speed:String = if (torrent.speed < 1024) torrent.speed+"B/s"
                           else if (torrent.speed < 1024*1024) (torrent.speed/1024)+"kB/s"
                           else (torrent.speed/(1024*1024))+"mB/s"
        table.update(row,0,torrent.name)
        table.update(row,1,complete)
        table.update(row,2,incomplete)
        table.update(row,3,peers)
        table.update(row,4,status)
        table.update(row,5,speed)
      }
    }
  }

}

class TorrentDisplay(val file:File) {
  val name: String = file.getName
  var incomplete:Int = -1
  var complete:Int = -1
  var peers:Int = -1
  var downloaded_pieces = -1
  var active:Int = 0
  var total_pieces = -1
  var speed:Float = 0

  def trackerUpdate(update:TrackerStatusUpdate): Unit = {
    if (incomplete == -1) incomplete = 0
    if (complete == -1) complete = 0
    if (peers == -1) peers = 0
    incomplete += update.incomplete
    complete += update.complete
    peers += update.peers
  }

  def peerManagerUpdate(update:PeerManagerUpdate): Unit = {
    downloaded_pieces = Math.max(downloaded_pieces,update.collected_pieces)
    total_pieces = update.total_pieces
  }

  def activePeersUpdate(update:ActivePeersUpdate): Unit = {
    active = update.peers
    speed = update.blocks * 16384
  }
}