package sbittorrent
import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import akka.routing.RoundRobinRouter

import scala.concurrent.duration.Duration

sealed trait PiMessage
case object Calculate extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage
case class PiApproximation(pi: Double, duration: Duration)

class Worker extends Actor {

  println("I was created!")

  override def receive: Receive = {
    case Work(start,nrOfElements) =>
      sender ! Result(calculatePiFor(start,nrOfElements))
  }

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1-(i%2)*2) / (2*i+1)
    acc
  }
}

class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef) extends Actor {
  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter: ActorRef = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)),
    name="workerRouter"
  )

  def receive = {
    case Calculate =>
      for (i <- 0 until nrOfMessages) workerRouter ! Work(i*nrOfElements,nrOfElements)
    case Result(value) =>
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        listener ! PiApproximation(pi,duration = Duration.fromNanos((System.currentTimeMillis - start)*1000))
        context.stop(self)
      }
  }
}

class Listener extends Actor {
  def receive = {
    case PiApproximation(pi, duration) =>
      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s".format(pi, duration))
      context.system.shutdown()
  }
}

object Pi extends App {
  calculate(nrOfWorkers=4,nrOfElements=10000,nrOfMessages=10000)

  def calculate(nrOfWorkers:Int,nrOfElements:Int,nrOfMessages:Int): Unit = {
    val system = ActorSystem("PiSystem")
    val listener = system.actorOf(Props[Listener], name="listener")
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)),name="master")
    master ! Calculate
  }
}