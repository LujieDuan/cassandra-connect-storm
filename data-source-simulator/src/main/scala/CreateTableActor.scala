import KeySpaceActor.Start
import akka.actor.{Actor, ActorLogging}
import collection.JavaConverters._

object CreateTableActor {
  case object Another
}

class CreateTableActor(table : java.util.LinkedHashMap[String, Object]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Start => {
      print("Start")
      for (action <- table.asScala) {

      }
    }
  }
}

