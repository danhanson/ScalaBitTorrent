package sbittorrent
import akka.actor.{Actor, ActorContext, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing._
import scala.concurrent.duration._

object SprayTutorial extends App {
  implicit val system = ActorSystem("smartjava")
  val service = system.actorOf(Props[SJServiceActor], "sj-rest-service")

  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, interface="localhost", port=8080)

}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)
}

case class Person(name:String,firstName:String,age:Long)

class SJServiceActor extends Actor with HttpService {
  import MyJsonProtocol._

  def actorRefFactory: ActorContext = context

  def receive: Receive = runRoute(aSimpleRoute ~ anotherRoute)

  val aSimpleRoute = {
    path("path1") {
      get {
        headerValue({
          case x@HttpHeaders.`Content-Type`(value) => Some(value)
          case default => None
        }) {
          header => header match {
            case ContentType(MediaType("application/vnd.type.a"), _) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type A", System.currentTimeMillis());
                }
              }
            }
            case ContentType(MediaType("application/vnd.type.b"), _) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type B", System.currentTimeMillis());
                }
              }
            }
            case default => {
              complete {
                HttpResponse(406);
              }
            }
          }
        }
      }
    }
  }

  val anotherRoute = {
    path("path2") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Path 2</h1>
              </body>
            </html>
          }
        }
      }
    }
  }
}