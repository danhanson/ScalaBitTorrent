/**
 * Created by michaea1 on 1/31/2015.
 */
class Parse extends ((List[String],Char) => List[String]) {

  var counter = 0
  var flag = false  // true -> still reading number

  override def apply(v1: List[String], v2: Char): List[String] = (counter,flag,v2) match {
    case (0,false,_) => {
      counter = v2-'0'
      flag = true
      v1
    }
    case (_,true,':') => {
      flag = false
      "" :: v1
    }
    case (_,true,_) => {
      counter = 10*counter+(v2-'0')
      v1
    }
    case (_,false,_) => {
      counter = counter - 1
      (v1.head+v2) :: v1.tail
    }
  }

}
