package fractal

import actors.Actor

// This actor will be started automatically. gui is a reference to the GUI
// actor, to which you should sent points you want drawn. See model.scala for
// the definition of the point class, and api.txt for a description of messages
// the GUI actor understands. See/edit conf.scala for display settings.
class ClientActor(gui:Actor) extends Actor{

	override def act(){
		//Your code goes here
		println("Hello, world!")
	}

}
