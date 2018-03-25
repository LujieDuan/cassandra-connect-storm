import KeySpaceActor.Start
import TableActor.{Act, Initialize}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import collection.JavaConverters._

object TableActor {
  case object Act
  case object Initialize
}

class TableActor(actions : java.util.ArrayList[String],
                 columns : java.util.ArrayList[String],
                 keySpace : String,
                 name : String,
                 delay : Int)
                (implicit sc: SparkContext) extends Actor with ActorLogging {

  val _rand = scala.util.Random

  var actionActors : Array[ActorRef] = Array.empty

  override def receive: Receive = {
    case Initialize => {
      log.info("Start Table Actor")
      createTable()

      actionActors = actions.asScala.zipWithIndex.map(action => action._1 match {
        case "insert" => startInsertActor(action._2)
        case "read" => startReadActor(action._2)
        case "update" => startUpdateActor(action._2)
        case "delete" => startDeleteActor(action._2)
      }).toArray

      context.system.scheduler.scheduleOnce(delay millis, self, Start)
    }

    case Start => {
      actionActors(_rand.nextInt(actionActors.length)) ! Act
      context.system.scheduler.scheduleOnce(delay millis, self, Start)
    }
  }

  private def createTable(): Unit = {
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"CREATE TABLE IF NOT EXISTS $keySpace.$name " +
        s"(${columnListToString()}, last_modified TIMESTAMP, last_modified_uuid TIMEUUID, " +
        s"PRIMARY KEY (${columns.get(0)})) WITH cdc = true")
    }
  }

  private def columnListToString(): String = {
    columns.asScala.map(a => a + " varchar").reduce((a, b) => a + ", " + b)
  }

  private def startInsertActor(i: Int): ActorRef = {
    context.actorOf(Props(
      new InsertActor(columns,
        name,
        keySpace)),
      s"InsertActor-$keySpace-$name-$i")
  }

  private def startUpdateActor(i: Int): ActorRef = {
    context.actorOf(Props(
      new UpdateActor(columns,
        name,
        keySpace)),
      s"UpdateActor-$keySpace-$name-$i")
  }

  private def startReadActor(i: Int): ActorRef = {
    context.actorOf(Props(
      new ReadActor(columns,
        name,
        keySpace)),
      s"ReadActor-$keySpace-$name-$i")
  }

  private def startDeleteActor(i: Int): ActorRef = {
    context.actorOf(Props(
      new DeleteActor(columns,
        name,
        keySpace)),
      s"DeleteActor-$keySpace-$name-$i")
  }
}

