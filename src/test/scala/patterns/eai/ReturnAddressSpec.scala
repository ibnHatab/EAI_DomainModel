package ro.cipex.eai.pattern.return_address

import org.scalatest.WordSpecLike
import org.scalatest.matchers._
import scala.language.postfixOps
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.event.LoggingReceive
import org.scalatest.Matchers


case class RequestComplex (what: String)
case class ReplyComplex (what: String)

class Worker extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case RequestComplex(what) =>
      sender ! ReplyComplex(s"RESP-1 for $what")
    case _ =>
      log.warning("Client recv: uexpected message")
  }
}

class Server extends Actor with ActorLogging {
  val worker = context.actorOf(Props[Worker], "worker")

  def receive = LoggingReceive {
    case request: RequestComplex =>
      worker forward request
    case _ =>
      log.warning("Client recv: uexpected message") 
  }
}

// Application
class ReturnAddressSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ReturnAddress" should {
    "allow create a child Worker" in {
      val probe = TestProbe()
      val server = system.actorOf(Props[Server], "Server")

      within(300 milliseconds) {

        probe.send(server, RequestComplex("JOB-1"))

        val result = probe.expectMsgType[ReplyComplex]
        result should equal(ReplyComplex("RESP-1 for JOB-1"))
      }        
    }
  }
}
