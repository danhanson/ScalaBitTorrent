import swing._
import actors._
import java.awt.image.BufferedImage

object App extends SimpleSwingApplication {

	val xSize = 512;
	val ySize = 512;
	var bound = Bound(-2.5, 2.5, -2.5, 2.5)

	var panel = new ImagePanel
	var actor = new GuiActor(xSize, ySize, bound, panel)

	def top = new MainFrame {
		title = "title"
		preferredSize = new Dimension(xSize, ySize)
		resizable = false
		contents = panel
		actor.start()
		(new testActor(actor)).start()
	}

}

class GuiActor(x:Int, y:Int, bound:Bound, panel:ImagePanel) extends Actor{
	var points:List[Point] = List()

	def act() {
		while (true) {
			receive {
				case point : Point => 
					println("received point")
					points = point :: points
					panel.draw(new ImageByPoints(x, y, bound, points))
					panel.repaint()
			}
		}
	}
}

class testActor(guiActor:Actor) extends Actor{

	def act(){
		Thread sleep 1000
		guiActor ! Point(0, 0, Color(255, 255, 255))
		Thread sleep 1000
		guiActor ! Point(1, 1, Color(255, 255, 255))
		Thread sleep 1000
		guiActor ! Point(2, 2, Color(255, 255, 255))
	}
}


class ImageByPoints(x:Int, y:Int, bound:Bound, points:Seq[Point]) 
extends BufferedImage(x, y, BufferedImage.TYPE_INT_RGB){
	val xInc = (bound.xMax - bound.xMin) / x
	val yInc = (bound.yMax - bound.yMin) / y

	points.filter(bound.contains).foreach( (p:Point) => {
		val xLoc = ((p.x - bound.xMin) / xInc).toInt
		val yLoc = ((p.y - bound.yMin) / yInc).toInt
		setRGB(xLoc, y-yLoc, p.color.toInt)
	})
}

class ImagePanel extends Panel{
	private var image:BufferedImage = null
	def draw(image:BufferedImage) = this.image=image

	override def paintComponent(g:Graphics2D) = if(image != null) g.drawImage(image, 0, 0, null)
}

case class Bound(xMin:Double, xMax:Double, yMin:Double, yMax:Double){
	def contains(p:Point) =
		(p.x >= xMin) && (p.x < xMax) && (p.y >= yMin) && (p.y < yMax)
}

case class Color(r:Int, g:Int, b:Int){
	def toInt = ((r & 0xFF) << 16) + ((g & 0xFF) << 8) + (b & 0xFF)
}

case class Point(x:Double, y:Double, color:Color)
