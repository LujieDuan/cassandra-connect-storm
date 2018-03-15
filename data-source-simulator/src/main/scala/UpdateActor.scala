import java.util.UUID

import TableActor.Act
import akka.actor.{Actor, ActorLogging}
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkContext
import com.datastax.spark.connector._
import collection.JavaConverters._

object UpdateActor {
}

class UpdateActor(columns : java.util.ArrayList[String],
                  tableName : String,
                  keySpaceName : String)
                 (implicit sc: SparkContext) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Act => {
      log.info("Updating")
      updateOne()
    }
  }

  private def updateOne(): Unit = {
    val rdd = sc.cassandraTable(keySpaceName, tableName).limit(1)
    val id = if (rdd.count() > 0) rdd.first().getString(0) else UUID.randomUUID().toString
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"UPDATE $keySpaceName.$tableName " +
        s"SET ${columnListToNewRecord()}, " +
        s"last_modified = toTimestamp(now()), " +
        s"last_modified_uuid = now()" +
        s"WHERE ${columns.get(0)} = '$id'")
    }
  }

  private def columnListToNewRecord(): String = {
    columns.asScala.drop(1).map(a => a + " = " + "'" + UUID.randomUUID().toString + "'").reduce((a, b) => a + ", " + b)
  }

}

