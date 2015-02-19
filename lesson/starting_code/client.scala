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
		new PrintBoundActor(gui).start()
	}

}

// This is an example actor. Whenever the gui's view area changes, this actor
// will print the new view area to the console.
class PrintBoundActor(gui:Actor) extends Actor{

	override def act(){
		gui ! "subscribe"
		while(true){
			receive {
				case b: Bound => println(b)
			}
		}
	}

}
