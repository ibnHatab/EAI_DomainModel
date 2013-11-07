package ro.cipex.eai.pattern.request_reply_spec

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


case class Request(what: String)
case class Reply(what: String)
case class StartWith(server: ActorRef) 

class Client extends Actor with ActorLogging {
  var probe: Option[ActorRef] = null

  def receive = LoggingReceive {
    case StartWith(server) => 
      log.debug(">> Client: is starting ...")
      probe = Some(sender)
      server ! Request("REQ-1")
    case Reply(what) =>
      for {
        origin <- probe
      } origin ! Reply(what)
    case _ =>
      log.warning("Client recv: uexpected message")
  }
}

class Server extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case Request(what) =>
      sender ! Reply(s"RESP-1 for $what")
    case _ =>
      log.warning("Client recv: uexpected message")
  }
}


// Application
class RequestReplySpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "Request-Reply" should {
    "allow two-way conversation" in {
      val probe = TestProbe()

      val client = system.actorOf(Props[Client], "Client")
      val server = system.actorOf(Props[Server], "Server")

      within(300 milliseconds) {

        probe.send(client, StartWith(server))

        val result = probe.expectMsgType[Reply]
        result should equal(Reply("RESP-1 for REQ-1"))
      }
    }
  }
}


