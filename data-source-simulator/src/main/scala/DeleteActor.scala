import java.util.UUID

import TableActor.Act
import akka.actor.{Actor, ActorLogging}
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkContext
import com.datastax.spark.connector._
object DeleteActor {
}

class DeleteActor(columns : java.util.ArrayList[String],
                  tableName : String,
                  keySpaceName : String)
                 (implicit sc: SparkContext) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Act => {
      log.info("Deleting")
      deleteOne()
    }
  }

  private def deleteOne(): Unit = {
    val rdd = sc.cassandraTable(keySpaceName, tableName).limit(1)
    val id = if (rdd.count() > 0) rdd.first().getString(0) else UUID.randomUUID().toString
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"DELETE FROM $keySpaceName.$tableName " +
        s"WHERE ${columns.get(0)} = '$id'")
    }
  }
}

