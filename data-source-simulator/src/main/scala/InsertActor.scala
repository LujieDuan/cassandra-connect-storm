import TableActor.Act
import akka.actor.{Actor, ActorLogging}
import java.util.UUID

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkContext

import collection.JavaConverters._

object InsertActor {
}

class InsertActor(columns : java.util.ArrayList[String],
                  tableName : String,
                  keySpaceName : String)
                 (implicit sc: SparkContext) extends Actor with ActorLogging {



  override def receive: Receive = {
    case Act => {
      log.info("Inserting")
      insertOne()
    }
  }

  private def insertOne(): Unit = {
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"INSERT INTO $keySpaceName.$tableName " +
        s"(${columnListToString()}, last_modified)" +
        s"VALUES (${generateRecord()}, toTimestamp(now()))")
    }
  }

  private def columnListToString(): String = {
    columns.asScala.reduce((a, b) => a + ", " + b)
  }

  private def generateRecord(): String = {
    columns.asScala.map(a => "'" + UUID.randomUUID().toString + "'").reduce((a, b) => a + ", " + b)
  }

}

