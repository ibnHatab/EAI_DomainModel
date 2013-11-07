package ro.cipex.eai.pattern.return_address

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.matchers._

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import com.typesafe.config.ConfigFactory
import akka.event.LoggingReceive
import akka.testkit.{ TestKit, TestProbe, ImplicitSender }

// import akka.persistence.{ Persistent, PersistenceFailure, Processor }

// Model
// case class Order(is: String)

// case class OrderReceived(id: String)

// class OrderProcessor extends Processor with ActorLogging {

//   def receive = LoggingReceive {
//     case Persistent(payload, sequenceNr) => {

//     }
//     case PersistenceFailure(payload, sequenceNr, cause) => {

//     }
//     case message =>
//       log.warning(s"$self: receive unexpected message $message")

//   }

// }

// // Application
// class ProcessorSpec extends TestKit(ActorSystem("EAI"))
//     with ImplicitSender with WordSpecLike with Matchers {
  
//   "Processor" should {
//     "replayed journaled messages" in {
//       val processor = system.actorOf(Props[OrderProcessor], "orderProcessor")

//       processor ! Order("2")

//     }
//   }
// }

