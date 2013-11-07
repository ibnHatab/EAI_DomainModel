package ro.cipex.eai.pattern.message_expiration

import scala.language.postfixOps
import scala.collection.mutable
import scala.concurrent.duration._

import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.event.LoggingReceive

import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe

trait ExpiringMessage {
  val occuredOn = System.currentTimeMillis
  val timeToLive: Long

  def isExpired = {
    val elapsed = System.currentTimeMillis
    elapsed > occuredOn + timeToLive
  }
}

case class PlaceOrder(id: String, itemId: String, timeToLive: Long) extends ExpiringMessage 
case class OrderReady(id: String)

class PurchaseAgent(client: ActorRef) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case message: PlaceOrder =>
      val ss = message.isExpired
      if(! message.isExpired) {
        client ! OrderReady(message.id)
      } else {
        context.system.deadLetters ! message
      }
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}

class PurchaseRouter(purchaseAgent: ActorRef) extends Actor with ActorLogging {
  val random = new scala.util.Random((new java.util.Date()).getTime)

  import context._

  def receive = LoggingReceive {
    case message: Any =>
      val delay = random.nextInt(100) + 1
      context.system.scheduler.scheduleOnce(delay millis, purchaseAgent, message)
  }
}

// Application
class MessageExpirationSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "MessageExpiration" should {
    "reroute expired messages to the Dead Letter" in {
      val probe = TestProbe()
      val purchaseAgent = system.actorOf(Props(new PurchaseAgent(probe.ref)), "PurchaseAgent")
      val purchaseRouter = system.actorOf(Props(new PurchaseRouter(purchaseAgent)), "PurchaseRouter")

      purchaseRouter ! PlaceOrder("1", "11", 1000)
      probe.expectMsgType[OrderReady].id should equal("1")

      purchaseRouter ! PlaceOrder("2", "12", 100)
      probe.expectMsgType[OrderReady].id should equal("2")

      purchaseRouter ! PlaceOrder("3", "13", 10)
      probe.expectNoMsg(1 second)
    }
  }
}
