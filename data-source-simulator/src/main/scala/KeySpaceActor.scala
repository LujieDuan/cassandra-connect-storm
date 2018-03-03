import KeySpaceActor.Start
import akka.actor.{Actor, ActorLogging, Props}
import collection.JavaConverters._

object KeySpaceActor {
  case object Start
}

class KeySpaceActor(keyspace : java.util.ArrayList[java.util.LinkedHashMap[String, Object]], name : String)
  extends Actor with ActorLogging {

  var index = 0

  override def receive: Receive = {
    case Start => {
      print("Start KeySpace")

      createKeySpace()

      for (table <- keyspace.asScala) {
        val tableName = table.get("name")
        val columns = table.get("column")
//        val tableActor = context.actorOf(Props(
//          new TableActor(table.get.asInstanceOf[java.util.LinkedHashMap[String, Object]], tableName)),
//          s"TableActor-$name-$index-$tableName")
//        index += 1
//        tableActor ! Start
      }
    }
  }

  private def createKeySpace(): Unit = {
    
  }
}

