package fractal

import actors.Actor

class ClientActor(gui:GuiActor) extends Actor{

	override def act(){
		for(x <- 0 until 8){
			new MandelbrotActor(Conf.bound, 200, 8, x, gui).start()
		}
	}

}

class MandelbrotActor(var bound:Bound, iterations:Int, totalActors:Int, thisActor:Int, gui:GuiActor) extends Actor{

	def mandelbrot(x:Double, y:Double, count:Int):Int = {
		var realPart:Double = 0
		var imPart:Double = 0

		for(iter <- 1 until count){
			val re = Math.pow(realPart, 2) - Math.pow(imPart, 2) + x
			val im = 2*realPart*imPart + y

			realPart = re
			imPart = im

			if((Math.abs(realPart) > 2) || (Math.abs(imPart) > 2))
				return iter
		}

		return count
	}

	def act(){
		var resolution = 2

		while(true){
			val xInc = (bound.xMax - bound.xMin) / resolution
			val yInc = (bound.yMax - bound.yMin) / resolution

			//var points:List[Point] = List()

			for(x <- 0 until (resolution - 1)){
				for(y <- 0 until (resolution - 1)){
					if( (x+y)%totalActors == thisActor) {
						val xP = bound.xMin + xInc * (x+0.5)
						val yP = bound.yMin + yInc * (y+0.5)

						val result = mandelbrot(xP, yP, iterations)
						if(result == iterations){
							gui ! Point(xP, yP, Color(0, 0, 0))
							//points = Point(xP, yP, Color(0, 0, 0)) :: points
						}else{
							val portion = (255* result / iterations).toInt
							gui ! Point(xP, yP, Color(portion, portion, portion))
							//points = Point(xP, yP, Color(portion, portion, portion)) :: points
						}
					}
				}
			}

			//gui ! points

			val otherBound = gui.getBound
			if(otherBound == bound){
				resolution = resolution * 2 + 1
			}else{
				bound = otherBound
				resolution = 2
			}
		}

	}
}
