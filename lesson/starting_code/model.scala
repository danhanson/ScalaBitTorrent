package fractal
import scala.math

case class Point(x:Double, y:Double, color:Color)

case class Color(r:Int, g:Int, b:Int){
	def toInt = ((r & 0xFF) << 16) + ((g & 0xFF) << 8) + (b & 0xFF)
}

case class Bound(xMin:Double, xMax:Double, yMin:Double, yMax:Double){
	// Bound contains a point
	def contains(p:Point) =
		(p.x >= xMin) && (p.x < xMax) && (p.y >= yMin) && (p.y < yMax)
	
	// Splits a bound into a list of count bounds covering the same area. The
	// resulting bounds are not guarenteed to all be the same size.
	def split(count:Int):List[Bound] = {
		var remaining = count
		val columns = math.ceil(count / math.sqrt(count)).toInt
		val cellsPerColumn = math.ceil(count.toDouble / columns).toInt

		var result:List[Bound] = List()

		val xInt = (xMax - xMin)/columns
		for(x <- 0 until columns){
			val cells = math.min(remaining, cellsPerColumn)
			val yInt = (yMax - yMin)/cells
			for(y <- 0 until cells){
				val newBound = Bound(xMin + xInt*x, xMin + xInt*(x+1), yMin + yInt*y, yMin + yInt*(y+1))
				result = newBound :: result
			}
			remaining -= cells
		}

		return result
	}

	// Returns this bound moved by a distance
	def up(diff:Double) = Bound(xMin, xMax, yMin+diff, yMax+diff)
	def down(diff:Double) = Bound(xMin, xMax, yMin-diff, yMax-diff)
	def right(diff:Double) = Bound(xMin+diff, xMax+diff, yMin, yMax)
	def left(diff:Double) = Bound(xMin-diff, xMax-diff, yMin, yMax)

	// Returns this bound zoomed in
	def out(factor:Double):Bound = {
		val xHeight = xMax - xMin
		val yHeight = yMax - yMin
		return Bound(xMin-xHeight*factor, xMax+xHeight*factor, yMin-yHeight*factor, yMax+yHeight*factor)
	}

	// Returns this bound zoomed out
	def in(factor:Double):Bound = {
		val xHeight = xMax - xMin
		val yHeight = yMax - yMin
		return Bound(xMin+xHeight*factor, xMax-xHeight*factor, yMin+yHeight*factor, yMax-yHeight*factor)
	}

}
