trait NoFuel
trait Fueled
trait NoOxygen
trait HasOxygen

case class Rocket[Fuel, Oxygen]()

object LaunchPad {

  def createRocket = Rocket[NoFuel, NoOxygen]()

  def addFuel[Oxygen](x: Rocket[NoFuel, Oxygen]) = {
    println("Adding fuel now")
    Rocket[Fueled, Oxygen]()
  }

  def addOxygen[Fuel](x: Rocket[Fuel, NoOxygen]) = {
    println("Adding oxygen now")
    Rocket[Fuel, HasOxygen]()
  }

  def launch(x : Rocket[Fueled, HasOxygen]) = "Blastoff!!"

}
