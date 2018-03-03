import KeySpaceActor.Start
import akka.actor.{Actor, ActorLogging}

object KeySpaceActor {
  case object Start
}

class KeySpaceActor() extends Actor with ActorLogging {

  override def receive: Receive = {
    case Start => print("Start")
  }
}

