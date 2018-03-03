import java.io.{File, FileInputStream, InputStream, InputStreamReader}

import KeySpaceActor.Start
import akka.actor.{ActorSystem, Props}
import org.yaml.snakeyaml.Yaml

/**
  *
  */
object Main extends App {

  val DEFAULT_PATH = "resources/dss.yml"
  val system = ActorSystem("data-source-simulator")

  // Java Yaml Reader
  val filePath = if (args.length > 0) args(0) else DEFAULT_PATH
  val yaml = new Yaml()
  val conf : java.util.LinkedHashMap[String, Object] = yaml.load(new FileInputStream(new File(filePath)))



  val k1 = system.actorOf(Props[KeySpaceActor], "k1")
  k1 ! Start
}
