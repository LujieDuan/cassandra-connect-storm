import java.io.{File, FileInputStream, InputStream, InputStreamReader}
import java.util

import KeySpaceActor.Start
import akka.actor.{ActorSystem, Props}
import org.yaml.snakeyaml.Yaml

import collection.JavaConverters._

/**
  *
  */
object Main extends App {

  val DEFAULT_PATH = "resources/dss.yml"
  val system = ActorSystem("data-source-simulator")

  // Java Yaml Reader
  val filePath = if (args.length > 0) args(0) else DEFAULT_PATH
  val yaml = new Yaml()
  val conf : java.util.LinkedHashMap[String, util.ArrayList[java.util.LinkedHashMap[String, Object]]] =
    yaml.load(new FileInputStream(new File(filePath)))
  var index = 0

  for (keyspace <- conf.get("keyspace").asScala) {
    val name = keyspace.get("name")
    val keySpaceActor = system.actorOf(Props(
      new KeySpaceActor(keyspace.get("table")
        .asInstanceOf[java.util.ArrayList[java.util.LinkedHashMap[String, Object]]],
        name.asInstanceOf[String])),
      s"KeySpaceActor-$index-$name")
    index += 1
    keySpaceActor ! Start
  }
}
