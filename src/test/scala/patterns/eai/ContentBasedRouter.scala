package ro.cipex.eai.pattern.content_based_router

import scala.language.postfixOps
import scala.collection.mutable

import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.event.LoggingReceive

import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe


case class Order(id: String, orderType: String)

case class OrderPlaced(client: ActorRef, order: Order)
case class OrderComplete(orderType: String)

class OrderProcessor(orderType: String) extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case OrderPlaced(origin, order) => 
      origin ! OrderComplete(orderType)
    case m =>
      log.warning(s"OrderProcessor of $orderType: receive unexpected message $m")
  }
}

class OrderRouter extends Actor with ActorLogging {
  val orderedTypeAProcessor =
    context.actorOf(Props(new OrderProcessor("TypeA")), "orderedTypeAProcessor")
  val orderedTypeBProcessor =
    context.actorOf(Props(new OrderProcessor("TypeB")), "orderedTypeBProcessor")

  def receive = LoggingReceive {
    case orderPlaced: OrderPlaced => orderPlaced.order.orderType match {
      case "TypeA" =>
        orderedTypeAProcessor ! orderPlaced
      case "TypeB" =>
        orderedTypeBProcessor ! orderPlaced
    }
    case m =>
      log.warning(s"OrderRouter: receive unexpected message $m")
  }
}

// Application
class ContentBasedRouter extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ContentBasedRouter" should {
    "route each message to the correct recipient" in {

      val client = TestProbe()

      val orderRouter = system.actorOf(Props[OrderRouter], "orederRouter")

      client.send(orderRouter, OrderPlaced(client.ref, Order("123", "TypeA")))
      client.expectMsg(OrderComplete("TypeA"))

      client.send(orderRouter, OrderPlaced(client.ref, Order("123", "TypeB")))
      client.expectMsg(OrderComplete("TypeB"))
    }
  }
}
