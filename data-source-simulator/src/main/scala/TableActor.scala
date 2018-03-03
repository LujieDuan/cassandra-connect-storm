import KeySpaceActor.Start
import akka.actor.{Actor, ActorLogging}
import collection.JavaConverters._

object TableActor {
  case object End
}

class TableActor(table : java.util.LinkedHashMap[String, Object], name : String) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Start => {
      print("Start")
      for (action <- table.entrySet().asScala) {

      }
    }
  }
}

