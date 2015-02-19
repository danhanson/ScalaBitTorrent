package sbittorrent
import java.io.File

import akka.actor.{Actor, ActorRef}

import scala.swing._
import scala.swing.event.ButtonClicked

// the filemanager constructor is so the GUI knows who to send
// messages to when you select a new file
class GUI(manager:ActorRef) extends SimpleSwingApplication with Actor {

  def top = new MainFrame {
    title = "sBitTorrent"
    val button = new Button {
      text = "new download"
    }
    val headers: Seq[String] = Array.tabulate(5) {"Col-"+_}.toSeq
    val rowData: Array[Array[Any]] = Array.tabulate[Any](10,10) ((_,_) => "")
    val table = new Table(rowData, headers) {
      selection.elementMode = Table.ElementMode.Cell
    }
    contents = new BoxPanel(Orientation.Vertical) {
      contents += button
      contents += table
      border = Swing.EmptyBorder(30,30,10,30)
    }
    listenTo(button)
    reactions += {
      case ButtonClicked(b) =>
        val fileChooser = new FileChooser(new File("input"))
        fileChooser.showOpenDialog(null)
        val file: File = fileChooser.selectedFile
        if (file != null) {   // closing the filechooser gives a null file
          manager ! file
        }
    }
  }

  // we will use this to send updates to the GUI
  // to show status of the torrents
  override def receive: Receive = {
    case "start" => {
      main(Array.empty[String])
    }
    case x => {
      println("SimpleSwingApplication received the message: " + x)
    }
  }
}
