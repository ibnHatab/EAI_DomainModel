package ro.cipex.eai.pattern.splitter

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

case class OrderedItem(id: String, itemType: String, description: String, price: Double) {
  override def toString = s"OrderedItem($id, $itemType, $description, $price)"
}

case class Order(id: String, orderedItems: Map[String, OrderedItem]) {
  def grandTotal: Double = orderedItems.values.foldLeft (0.0) (_+_.price)
//    orderedItems.values.foldLeft (0.0) ((sum, item) => sum + item.price)
// orderedItems.values.map( _.price ).sum
  override def toString = s"Order($orderedItems) with total: $grandTotal"
}

case class OrderPlaced(client: ActorRef, order: Order)
case class OrderComplete()
case class TypeAItemOrdered(orderId: String, orderedItems: OrderedItem) 
case class TypeBItemOrdered(orderId: String, orderedItems: OrderedItem) 
case class TypeCItemOrdered(orderId: String, orderedItems: OrderedItem)
case class OrderedItemProcessed(orderId: String, itemId: String)


class OrderRouter extends Actor with ActorLogging {
  val orderedItemTypeAProcessor =
    context.actorOf(Props[OrderedItemTypeAProcessor], "orderedItemTypeAProcessor")
  val orderedItemTypeBProcessor =
    context.actorOf(Props[OrderedItemTypeBProcessor], "orderedItemTypeBProcessor")
  val orderedItemTypeCProcessor =
    context.actorOf(Props[OrderedItemTypeCProcessor], "orderedItemTypeCProcessor")

  val orderRegistry = new mutable.HashSet[(String, String)]
  val clientRegistry = new mutable.HashMap[String, ActorRef]

  def receive = LoggingReceive {
    case OrderPlaced(origin, order) => order.orderedItems foreach {
      case (itemType, orderedItem) => itemType match {
        case "TypeA" => orderedItemTypeAProcessor ! TypeAItemOrdered(order.id, orderedItem)
        case "TypeB" => orderedItemTypeBProcessor ! TypeBItemOrdered(order.id, orderedItem)
        case "TypeC" => orderedItemTypeCProcessor ! TypeCItemOrdered(order.id, orderedItem)
      }
        orderRegistry += ((order.id, orderedItem.id))
    }
      clientRegistry += order.id -> origin
    case OrderedItemProcessed(orderId, itemId) =>
      orderRegistry -= ((orderId, itemId))
      val itemsToPocess = orderRegistry.find {case ((oid, _)) => oid == orderId }

      if(itemsToPocess.isEmpty)
        clientRegistry(orderId) ! OrderComplete

    case m =>
      log.warning(s"OrderRouter: receive unexpected message $m")
  }
}

class OrderedItemTypeAProcessor extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case TypeAItemOrdered(orderId, orderedItem) =>
      sender ! OrderedItemProcessed(orderId, orderedItem.id)
    case m =>
      log.warning(s"OrderedItemTypeAProcessor: receive unexpected message $m")
  }
}

class OrderedItemTypeBProcessor extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case TypeBItemOrdered(orderId, orderedItem) =>
      sender ! OrderedItemProcessed(orderId, orderedItem.id)
    case m =>
      log.warning(s"OrderedItemTypeBProcessor: receive unexpected message $m")
  }
}

class OrderedItemTypeCProcessor extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case TypeCItemOrdered(orderId, orderedItem) =>
      sender ! OrderedItemProcessed(orderId, orderedItem.id)
    case m =>
      log.warning(s"OrderedItemTypeCProcessor: receive unexpected message $m")
  }
}

// Application
class SplitterSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "Splitter" should {
    "break out the composite message into a series of individual messages" in {

      val client = TestProbe()

      val orderRouter = system.actorOf(Props[OrderRouter], "orederRouter")

      val orderedItem1 = OrderedItem("1", "TypeA", "An item of type A", 42.0)
      val orderedItem2 = OrderedItem("2", "TypeB", "An item of type B", 24.0)
      val orderedItem3 = OrderedItem("3", "TypeC", "An item of type C", 45.0)

      val orderedItems = Map(
        orderedItem1.itemType -> orderedItem1,
        orderedItem2.itemType -> orderedItem2,
        orderedItem3.itemType -> orderedItem3
      )

      client.send(orderRouter, OrderPlaced(client.ref, Order("01", orderedItems)))
 
      client.expectMsg(OrderComplete)
    }
  }
}

