import java.util.UUID
import TableActor.Act
import akka.actor.{Actor, ActorLogging}
import org.apache.spark.SparkContext
import com.datastax.spark.connector._

object ReadActor {
}

class ReadActor(column : java.util.ArrayList[String],
                tableName : String,
                keySpaceName : String)
               (implicit sc: SparkContext) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Act => {
      log.info("Reading")
      readOne()
    }
  }

  private def readOne(): Unit = {
    val rdd = sc.cassandraTable(keySpaceName, tableName).limit(1)
    val id = if (rdd.count() > 0) rdd.first().getString(0) else UUID.randomUUID().toString
    log.info(s"Read record with id $id")
  }
}

