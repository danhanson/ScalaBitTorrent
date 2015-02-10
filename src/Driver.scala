import scala.swing._

val frame = new MainFrame {
  title = "sBitTorrent"
//  contents = Button("Click me")(println("Button was clicked"))
  contents = new TextArea
  size = new Dimension(500,500)
  centerOnScreen()
}

frame.visible = true