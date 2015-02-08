import java.util

trait NoFuel
trait HasFuel extends NoFuel
trait HasExtraFuel extends HasFuel
trait NoDamage
trait MinorDamage extends NoDamage
trait MajorDamage extends MinorDamage

case class Rocket[+Fuel, -Damage]()

object LaunchPad {
  def createRocket = Rocket[NoFuel, NoDamage]()

  def addFuel[Damage](x: Rocket[NoFuel, Damage]) = {
    println("Adding fuel now")
    Rocket[HasFuel, Damage]()
  }

  def addLotsOfFuel[Damage](x: Rocket[HasFuel, Damage]) = {
    println("Adding even more fuel!")
    Rocket[HasExtraFuel, Damage]
  }

  def loosenLandingGear[Fuel](x: Rocket[Fuel, NoDamage]) = {
    println("Landing gear fell off")
    Rocket[Fuel, MinorDamage]
  }

  def hitByTrain[Fuel, PriorDamage](x: Rocket[Fuel, PriorDamage]) = {
    println("Rocket hit by derailed train")
    Rocket[Fuel, MajorDamage]
  }

  def launch(x : Rocket[HasFuel, MinorDamage]) = {
    println("BLASTOFF!!!")
    Rocket[NoFuel, MinorDamage]()
  }

}
