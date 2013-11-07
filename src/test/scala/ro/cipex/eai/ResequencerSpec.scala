package ro.cipex.eai.resequencer

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

case class SequencedMessage(correlatioId: String, index: Int) 

class MessageSequencer(sinc: ActorRef) extends Actor with ActorLogging {
  val messageMap = mutable.Map[String, (Int, List[SequencedMessage])]().withDefaultValue( (0, List()) )

  def sequence(queue: List[SequencedMessage], message: SequencedMessage) =
    queue.::(message).sortWith((lhs, rhs) => lhs.index < rhs.index)

  def unsequence(from: Int, queue: List[SequencedMessage]): (Int, List[SequencedMessage]) = 
     queue match {
      case Nil => (from, List())
      case x::xs =>
        if(x.index == from) {
          sinc ! x
          unsequence(from + 1, xs)
        } else {
          (from, queue)
        }
    }
  
  def receive = LoggingReceive {
    case message: SequencedMessage =>
      val (curIndex, queue) = messageMap(message.correlatioId)
      messageMap(message.correlatioId) = unsequence(curIndex, sequence(queue, message))
    case m =>
       log.warning(s"$self: receive unexpected $m")
  }
}

// Application
class ResequencerSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "Resequencer" should {
    val probe = TestProbe()
    val sequencer = system.actorOf(Props(new MessageSequencer(probe.ref)), "sequencer")

    "preserve ordered" in {

      for (index <- 0 to 4) {
        sequencer ! SequencedMessage("ABC", index)
      }

      for (index <- 0 to 4) {
        probe.expectMsgType[SequencedMessage].index should equal(index)
      }

    }

    "collect and re-order messages" in {

      val mix = for (index <- 0 to 4) yield SequencedMessage("XYZ", index)

      for (message <- scala.util.Random.shuffle(mix)) {
        println(s">> $message")
        sequencer ! message
      }


      for (index <- 0 to 4) {
        probe.expectMsgType[SequencedMessage].index should equal(index)
      }
    
    }
  }
}
