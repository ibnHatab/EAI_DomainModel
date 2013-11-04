package ro.cipex.eai.dynamic_router

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

case class InterestedIn(messageType: String)
case class NoLongerInterestedIn(messageType: String)

case class TypedMessage(messageType: String, description: String)
case class TypedMessageConfirmation(messageType: String)

class DonnoInterested extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case message: Any =>
      log.error(s"DonnoInterested: in $message")
  }
}

class TypeInterested(messageType: String, backEnd: ActorRef, interestRouter: ActorRef)
    extends Actor with ActorLogging {

  override def preStart(): Unit = {
    interestRouter ! InterestedIn(messageType)
  }

  def receive = LoggingReceive {
    case message @ TypedMessage(messageType, _) if messageType == this.messageType =>
      backEnd ! TypedMessageConfirmation(message.messageType)
    case m: Any =>
      log.error(s"TypeInterested: receive unexpected $m")
  }

  override def postStop(): Unit = {
      log.debug(s"TypeInterested: stopping")
      interestRouter ! NoLongerInterestedIn(messageType)
  }
}

class TypedMessageInterestRouter(donnoInterested: ActorRef) extends Actor with ActorLogging {

  val interestRegistry = mutable.Map[String, List[ActorRef]]().withDefaultValue(List())

  def receive = LoggingReceive {
    case interestedIn: InterestedIn =>
      registerInterest(interestedIn)
    case noLongerInterestedIn: NoLongerInterestedIn =>
      unregisterInterest(noLongerInterestedIn)
    case message: TypedMessage =>
      sendFor(message)
  }
 
  def registerInterest(interestedIn: InterestedIn) = 
    interestRegistry(interestedIn.messageType) = sender :: interestRegistry(interestedIn.messageType)

  def unregisterInterest(noLongerInterestedIn: NoLongerInterestedIn) = {
    if (interestRegistry.contains(noLongerInterestedIn.messageType)) {

      interestRegistry(noLongerInterestedIn.messageType) =
        interestRegistry(noLongerInterestedIn.messageType) filter (sender !=)
    }
  }

  def sendFor(message: TypedMessage) = {
    if (interestRegistry.contains(message.messageType) &&
      ! interestRegistry(message.messageType).isEmpty) {

      interestRegistry(message.messageType).head ! message
    } else {
      donnoInterested ! message
    }
  }

}

// Application
class DynamicRouterRouter extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "DynamicRouter" should {
    val donnoInterested = system.actorOf(Props[DonnoInterested], "DonnoInterested")

    val typedMessageInterestRouter = system.actorOf(Props(
      new TypedMessageInterestRouter(donnoInterested)), "typedMessageInterestRouter")

    val backEnd = TestProbe()

    val typeAInterested = system.actorOf(
      Props(new TypeInterested("TypeA", backEnd.ref, typedMessageInterestRouter)), "typeAInterested")
    val typeCInterested = system.actorOf(
      Props(new TypeInterested("TypeC", backEnd.ref, typedMessageInterestRouter)), "typeCInterested")
    val typeCInterestedSecond = system.actorOf(
      Props(new TypeInterested("TypeC", backEnd.ref, typedMessageInterestRouter)), "typeCInterestedSecond")


    "can self-configure based on special configuration messages from participating destinations" in {
      typedMessageInterestRouter ! TypedMessage("TypeA", "registration test message")
      backEnd.expectMsgType[TypedMessageConfirmation].messageType should equal("TypeA")
    }

    "route each message to the correct recipient" in {
      typedMessageInterestRouter ! TypedMessage("TypeC", "message for Second TypeC")

      backEnd.expectMsgType[TypedMessageConfirmation].messageType should equal("TypeC")

      system.stop(typeCInterestedSecond)

      typedMessageInterestRouter ! TypedMessage("TypeC", "message for First TypeC")
      backEnd.expectMsgType[TypedMessageConfirmation].messageType should equal("TypeC")
    }

    "should deliver dead letter to donno actor" in {
      typedMessageInterestRouter ! TypedMessage("TypeD", "dead letter of TypeD")
      backEnd.expectNoMsg(500 millis)
    }
  }
}


