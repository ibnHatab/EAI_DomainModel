package patterns.eai.processor

import scala.language.postfixOps
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

case class Status() 
case class Snapshot()

class MyProcessor extends Processor with ActorLogging{
  val destination = context.actorOf(Props[MyDesctination], name="destination")
  val channel = context.actorOf(Channel.props(), name="channel")

  var state = java.util.UUID.randomUUID.toString()

  override def preStart() = {
//     self ! Recover() 
  }
 
  def receive = LoggingReceive {
    case p @ Persistent(payload, sequenceNr) =>
      channel ! Deliver(p.withPayload(s"processed: ${payload}"), destination)

    case Status =>
      log.info(s"My $processorId, $recoveryRunning")

//    case Snapshot => this.// saveSnapshot(state)

    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}

class MyDesctination extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case p @ Persistent(payload, sequenceNr) =>
      log.debug(s"receive: $payload, $sequenceNr")
      p.confirm()
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }

}

// Application
class ProcessorSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {

  "Processor" should {
    "persist and report" in {

      val processor = system.actorOf(Props[MyProcessor], name="processor")

      processor ! Persistent("foo")
      processor ! "bar"
      processor ! Status

      system.shutdown()
    }
  }
}
