package patterns.a_es.event_sourcing

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

case class Cmd(data: String)
case class Evt(data: String)

case class ExState(events: List[String] = Nil) {
  def update(evt: Evt) = copy(evt.data :: events)
  def size = events.size
  override def toString = events.reverse.toString
}

class ExProcessor extends EventsourcedProcessor with ActorLogging {
  val state = ExState()
 
  def update(evt: Evt) = state.update(evt)

  def numEvents = state.size

  def receiveCommand: PartialFunction[Any,Unit] = { case _ => () }
  def receiveReplay: PartialFunction[Any,Unit] = { case _ => () }


}

// Application
class EventSourcingSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {

  "EventSourcing" should {

    "Save Domain Events" in {

    }
    system.shutdown()
  }
}

