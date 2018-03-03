import KeySpaceActor.Start
import akka.actor.{ActorSystem, Props}

/**
  *
  */
object Main extends App {
  val system = ActorSystem("data-source-simulator")
  val k1 = system.actorOf(Props[KeySpaceActor], "k1")
  k1 ! Start
}
