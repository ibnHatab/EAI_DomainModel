package ro.cipex.eai.claim_check

import scala.language.postfixOps
sbt
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

case class Part(name: String)

case class ClaimCheck() {
  val number = java.util.UUID.randomUUID().toString()
  override def toString = s"ClaimCheck: $number"
}

case class CompositeMessage(id: String, part1: Part, part2: Part, part3: Part)

case class ProcessStep(id: String, claimCheck: ClaimCheck)

case class StepCompleted(id: String, claimCheck: ClaimCheck, stepName: String)

case class CheckedItem(claimCheck: ClaimCheck, bussinesId: String, parts: Map[String, Any])

case class CheckedPart(claimCheck: ClaimCheck, partName: String, parts: Any)

class ItemChecker {
  val checkedItems = mutable.Map[ClaimCheck, CheckedItem]()

  def checkItemFor(bussinesId: String, parts: Map[String, Any]) = CheckedItem(ClaimCheck(), bussinesId, parts)

  def checkItem(item: CheckedItem) = checkedItems.update(item.claimCheck, item)

  def claimItem(claimCheck: ClaimCheck) = checkedItems(claimCheck)

  def claimPart(claimCheck: ClaimCheck, partName: String) = {
    val checkedItem = checkedItems(claimCheck)
    CheckedPart(claimCheck, partName, checkedItem.parts(partName))
  }

  def removeItem(claimCheck: ClaimCheck) =
    if(checkedItems.contains(claimCheck))
      checkedItems.remove(claimCheck)
}

class Step(itemChecker: ItemChecker, spec: String) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case processStep: ProcessStep =>
      val claimPart = itemChecker.claimPart(processStep.claimCheck, "part" + spec)
      log.debug(s"$self: using $claimPart")
      sender ! StepCompleted(processStep.id, processStep.claimCheck, "step" + spec)
    case m => 
      log.warning(s"$self: receive unexpected $m")
  }
}

class Process(steps: Seq[ActorRef], itemChecker: ItemChecker)
    extends Actor with ActorLogging {
  var stepIndex = 0

  def receive = LoggingReceive {
    case message: CompositeMessage =>
      val parts = Map(
        message.part1.name -> message.part1,
        message.part2.name -> message.part2,
        message.part3.name -> message.part3
      )

      val checkedItem = itemChecker.checkItemFor(message.id, parts)

      itemChecker.checkItem(checkedItem)

      steps(stepIndex) ! ProcessStep(message.id, checkedItem.claimCheck)

    case message: StepCompleted =>
      stepIndex += 1

      if (stepIndex < steps.size) {
        steps(stepIndex) ! ProcessStep(message.id, message.claimCheck)        
      } else {
        itemChecker.removeItem(message.claimCheck)
      }

    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}


// Application
class ClaimCheckSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ClaimCheck" should {
    val itemChecker = new ItemChecker()

    "Store message data in a persistent store" in {
      val step1 = system.actorOf(Props(new Step(itemChecker, "A")), "step1")
      val step2 = system.actorOf(Props(new Step(itemChecker, "B")), "step2")
      val step3 = system.actorOf(Props(new Step(itemChecker, "C")), "step3")

      val process = system.actorOf(
        Props(new Process(Vector(step1, step2, step3), itemChecker)), "process")

      process ! CompositeMessage("ABC", Part("partA"), Part("partB"), Part("partC"))

    }
  }
}
