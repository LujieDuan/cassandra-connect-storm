import KeySpaceActor.Start
import TableActor.Initialize
import akka.actor.{Actor, ActorLogging, Props}

import collection.JavaConverters._
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkContext

object KeySpaceActor {
  case object Start
}

class KeySpaceActor(keyspace : java.util.ArrayList[java.util.LinkedHashMap[String, Object]], name : String)
                   (implicit sc: SparkContext) extends Actor with ActorLogging {

  var index = 0

  override def receive: Receive = {
    case Start => {
      log.info("Start KeySpace")

      createKeySpace()

      for (table <- keyspace.asScala) {
        val tableName = table.get("name")
        val columns = table.get("column")
        val delay = table.getOrDefault("delay", 100.asInstanceOf[AnyRef])
        val tableActor = context.actorOf(Props(
          new TableActor(table.get("action")
            .asInstanceOf[java.util.ArrayList[String]],
            columns.asInstanceOf[java.util.ArrayList[String]],
            name,
            tableName.asInstanceOf[String],
            delay.asInstanceOf[Int])),
          s"TableActor-$name-$index-$tableName")
        index += 1
        tableActor ! Initialize
      }
    }
  }

  private def createKeySpace(): Unit = {
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $name " +
        "WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    }
  }
}

