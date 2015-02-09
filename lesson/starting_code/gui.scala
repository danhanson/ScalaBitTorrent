package fractal

import swing._
import swing.event._
import actors._
import java.awt.image.BufferedImage
import java.awt.image.SinglePixelPackedSampleModel
import java.awt.image.Raster
import java.awt.image.DataBuffer

object Fractal extends SimpleSwingApplication {

	val xSize = Conf.xSize
	val ySize = Conf.ySize
	var bound = Conf.bound

	var panel = new ImagePanel
	var actor = new GuiActor(xSize, ySize, bound, panel)
	panel.setActor(actor)

	def top = new MainFrame {
		title = "Fractal"
		preferredSize = new Dimension(xSize, ySize)
		resizable = false
		contents = panel
		actor.start()
		(new ClientActor(actor)).start()
	}

}

class GuiActor(x:Int, y:Int, bound:Bound, panel:ImagePanel) extends Actor{
	val image = new MutableImageByPoints(x, y, bound, Color(255, 255, 255))
	var subscribers:List[OutputChannel[Any]] = List()
	panel.draw(image)

	def act() {
		while (true) {
			receive {
				case points : Iterable[Point] =>
					points.foreach(image.drawPoint)
					panel.repaint
				case point : Point => 
					image.drawPoint(point)
					panel.repaint
				case bound : Bound =>
					image.setBound(bound)
					panel.repaint
					notifySubscribers
				case "getBound" =>
					sender ! image.getBound
				case "subscribe" =>
					subscribers = sender :: subscribers
			}
		}
	}

	private def notifySubscribers = {
		subscribers.foreach( (x) => x ! image.getBound )
	}

	def getBound = image.getBound
}

class MutableImageByPoints(x:Int, y:Int, var bound:Bound, color:Color) 
extends BufferedImage(x, y, BufferedImage.TYPE_INT_RGB){

	var xInc = 0.0
	var yInc = 0.0

	updateIncrements

	def drawBg = {
		for(xi <- 1 until x){
			for(yi <- 1 until y){
				setRGB(xi, y-yi, color.toInt)
			}
		}
	}

	def setBound(bound:Bound) = {
		this.bound = bound
		updateIncrements
	}

	def updateBound = {
		xInc = (bound.xMax - bound.xMin) / x
		yInc = (bound.yMax - bound.yMin) / y
		drawBg
	}

	def drawPoint(point:Point) = {
		if(bound.contains(point)){
			val xLoc = ((point.x - bound.xMin) / xInc).toInt
			val yLoc = 1 + ((point.y - bound.yMin) / yInc).toInt
			setRGB(xLoc, y-yLoc, point.color.toInt)
			//points = point :: points
		}

	}

	def getBound = bound

}

class ImagePanel extends Panel{
	private var image:MutableImageByPoints = null
	def draw(image:MutableImageByPoints) = {this.image=image; repaint}

	override def paintComponent(g:Graphics2D) = if(image != null) g.drawImage(image, 0, 0, null)

	var actor:Actor = null
	def setActor(a:Actor) = actor=a

	listenTo(keys)
	reactions += {
		case KeyPressed(_, Key.Down, _, _) => {
			val b = image.getBound
			actor ! (b.down( (b.yMax - b.yMin)/3))
		}
		case KeyPressed(_, Key.Up, _, _) => {
			val b = image.getBound
			actor ! (b.up( (b.yMax - b.yMin)/3))
		}
		case KeyPressed(_, Key.Right, _, _) => {
			val b = image.getBound
			actor ! (b.right( (b.xMax - b.xMin)/3))
		}
		case KeyPressed(_, Key.Left, _, _) => {
			val b = image.getBound
			actor ! (b.left( (b.xMax - b.xMin)/3))
		}
		case KeyPressed(_, Key.I, _, _) =>
			actor ! image.getBound.in(0.2)
		case KeyPressed(_, Key.O, _, _) =>
			actor ! image.getBound.out(0.2)
	}

	focusable = true
	requestFocus
}
