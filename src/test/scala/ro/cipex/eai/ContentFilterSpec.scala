package ro.cipex.eai.content_filter

import scala.language.postfixOps

import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.event.LoggingReceive

import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe

case class FilteredMessage(light: String, and: String, fluffy: String, message: String) {
  override def toString = {
    s"FilteredMessage $light, and $fluffy message"
  }
}

case class UnfilteredMessage(largePayload: String)

class MessageContentFilter extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case message: UnfilteredMessage =>
      sender ! FilteredMessage("this", "feels", "so", "right")
    case _ =>
      log.warning("MessageContentFilter: unexpected message")
  }
}

class MessageExchangeDispatcher(origin: ActorRef) extends Actor with ActorLogging {
  val messageContentFilter = context.actorOf(Props[MessageContentFilter], "messageContentFilter")

  def receive = LoggingReceive {
    case messgage: UnfilteredMessage =>
      messageContentFilter ! messgage
    case messgage: FilteredMessage =>
      origin ! messgage
    case _ =>
      log.warning("MessageExchangeDispatcher: unexpected messgage")
  }
}

// Application
class ContentFilterSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ContentFilter" should {
    "remove unimportant data items from a message" in {

      val probe = TestProbe()

      val messageExchangeDispatcher =
        system.actorOf(Props(new MessageExchangeDispatcher(probe.ref)), "messageExchangeDispatcher")

      messageExchangeDispatcher ! UnfilteredMessage("A wary large message with complex structure ...")

      val res = probe.expectMsgType[FilteredMessage]        

    }}
}
