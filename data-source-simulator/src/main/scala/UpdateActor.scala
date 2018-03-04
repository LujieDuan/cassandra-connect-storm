import TableActor.Act
import akka.actor.{Actor, ActorLogging}
import collection.JavaConverters._

object UpdateActor {
}

class UpdateActor(column : java.util.ArrayList[String],
                  tableName : String,
                  keySpaceName : String) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Act => {
      print("U A")
    }
  }
}

