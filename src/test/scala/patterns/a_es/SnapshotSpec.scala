package patterns.a_es.snapshot

import scala.concurrent._
import scala.concurrent.duration._

import akka.pattern.ask
import akka.util.Timeout

import akka.actor.{ Actor, Props, ActorSystem, ActorLogging }
import akka.event.LoggingReceive

import akka.persistence._

import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.Timeout

case class ExampleState(received: List[String] = Nil) {
  def update(s: String) = copy(s :: received)
  override def toString = received.reverse.toString
}

class ExampleProcessor extends Processor {
  var state = ExampleState()

  def receive = {
    case Persistent(s, snr)                    ⇒ state = state.update(s"${s}-${snr}")
    case SaveSnapshotSuccess(metadata)         ⇒ // ...
    case SaveSnapshotFailure(metadata, reason) ⇒ // ...
    case SnapshotOffer(_, s: ExampleState)     ⇒ { println("offered state = " + s); state = s }
    case "print"                               ⇒ println("current state = " + state)
    case "snap"                                ⇒ saveSnapshot(state)
  }
}

// Application
class SnapshotSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {

  "Snapshot" should {
    val processor = system.actorOf(Props(classOf[ExampleProcessor]), "processor-3-scala")

    "do stuff" in {

      processor ! Persistent("a")
      processor ! Persistent("b")
      processor ! "snap"
      processor ! Persistent("c")
      processor ! Persistent("d")
      processor ! "print"

      Thread.sleep(1000)
        system.shutdown()
    }
  }
}
