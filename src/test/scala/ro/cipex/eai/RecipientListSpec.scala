package ro.cipex.eai.recipient_list

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

// Domain Model

case class RetailItem(itemId: String, retailPrice: Double)

case class PriceQuote(rfqId: String, itemId:String, retailPrice: Double, discountPrice: Double)

case class PriceQuoteInterest(
  path: String,
  quoteProcessor: ActorRef,
  lowTotalRetail: Double,
  heighTotalRetail: Double)

case class RequestPriceQuote(
  rfqId: String,
  itemId: String,
  retailPrice: Double,
  orderTotalRetailPrice: Double)

abstract class RetailPriceQuotes(
  orderProcessor: ActorRef,
  retailPrice: Double,
  discountPrice: Double) extends Actor with ActorLogging {

  def discountPercentage(orderedTotalRetailPrice: Double): Double

  override def preStart(): Unit = {
    orderProcessor ! PriceQuoteInterest(self.path.toString, self, retailPrice, discountPrice)
  }

  def receive = LoggingReceive {
    case rpq: RequestPriceQuote  =>
      val discount = discountPercentage(rpq.orderTotalRetailPrice) * rpq.retailPrice
      sender ! PriceQuote(rpq.rfqId, rpq.itemId, rpq.retailPrice, rpq.retailPrice - discount)

    case m =>
      log.warning(s"$self: receive unexpected $m")

  }
}

class BudgetHikersPriceQuotes(orderProcessor: ActorRef)
    extends RetailPriceQuotes (orderProcessor, 1.00, 1000.00) {
  def discountPercentage(orderedTotalRetailPrice: Double) = 0.2
}
class HighSierraPriceQuotes(orderProcessor: ActorRef)
    extends RetailPriceQuotes (orderProcessor, 2.00, 2000.00) {
  def discountPercentage(orderedTotalRetailPrice: Double) = 0.3
}
class MountainAscentPriceQuotes(orderProcessor: ActorRef)
    extends RetailPriceQuotes (orderProcessor, 2.00, 2000.00) {
  def discountPercentage(orderedTotalRetailPrice: Double) = 0.4
}
class PinnacleGearPriceQuotes(orderProcessor: ActorRef)
    extends RetailPriceQuotes (orderProcessor, 2.00, 2000.00) {
  def discountPercentage(orderedTotalRetailPrice: Double) = 0.5
}
class RockBottomOuterwearPriceQuotes(orderProcessor: ActorRef)
    extends RetailPriceQuotes (orderProcessor, 2.00, 2000.00) {
  def discountPercentage(orderedTotalRetailPrice: Double) = 0.6
}

// Engine
case class RequestForQuotation(rfqId: String, retailItems: Seq[RetailItem]) {
  def totalRetailPrice = retailItems.foldLeft(0.0) (_ + _.retailPrice)
}

class MountaineeringSupplierOrderProcessor(priceQuoteAggregator: ActorRef) extends Actor with ActorLogging {
  val interestRegister = mutable.Map[String, PriceQuoteInterest]()
  val clientRegister = mutable.Map[String, ActorRef]()

  def calsulatePriceRecipientList(rfq: RequestForQuotation): Iterable[ActorRef] = {
    for {
      interest <- interestRegister.values
      if(rfq.totalRetailPrice >= interest.lowTotalRetail)
      if(rfq.totalRetailPrice <= interest.heighTotalRetail)
    } yield interest.quoteProcessor
  }

  def dispatchTo(rfq: RequestForQuotation, recipientList: Iterable[ActorRef]) = {
    for (
      recipient <- recipientList;
      retailItem <- rfq.retailItems
    ) {
      recipient ! RequestPriceQuote(rfq.rfqId, retailItem.itemId, retailItem.retailPrice, rfq.totalRetailPrice)
    }
  }

  def receive = LoggingReceive {
    case interest: PriceQuoteInterest =>
      interestRegister(interest.path) = interest

    case priceQuote: PriceQuote =>
      priceQuoteAggregator ! PriceQuoteFullFilled(priceQuote)

    case rfq: RequestForQuotation  =>
      clientRegister(rfq.rfqId) = sender
      val recipientList = calsulatePriceRecipientList(rfq)
      priceQuoteAggregator ! RequiredPriceQuoteForFullfillement(rfq.rfqId, recipientList.size * rfq.retailItems.size)
      dispatchTo(rfq, recipientList)

    case fulfillment: QuotationFullfilement =>
      clientRegister(fulfillment.rfqId) ! fulfillment
      clientRegister.remove(fulfillment.rfqId)

    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}

// Aggregator Pattern
case class PriceQuoteFullFilled(priceQuote: PriceQuote)

case class RequiredPriceQuoteForFullfillement(rfqId: String, quotqRequested: Int)

case class QuotationFullfilement(rfqId: String, quoteRequsted: Int, priceQuotes: Seq[PriceQuote], requeter: ActorRef)

class PriceQuoteAggregator extends Actor with ActorLogging {
  val fullfiledPriceQuotes = mutable.Map[String, QuotationFullfilement]()

  def receive = LoggingReceive {
    case required: RequiredPriceQuoteForFullfillement =>
      fullfiledPriceQuotes(required.rfqId) = QuotationFullfilement(required.rfqId, required.quotqRequested, Vector(), sender)

    case priceQuoteFullFilled: PriceQuoteFullFilled =>
      val previoseFullfilement = fullfiledPriceQuotes(priceQuoteFullFilled.priceQuote.rfqId)        
      val currentPriceQuoted = previoseFullfilement.priceQuotes :+ priceQuoteFullFilled.priceQuote
      val currentFullfilement = previoseFullfilement.copy(priceQuotes = currentPriceQuoted)

      if(currentPriceQuoted.size >= currentFullfilement.quoteRequsted) {
        currentFullfilement.requeter ! currentFullfilement
        fullfiledPriceQuotes.remove(priceQuoteFullFilled.priceQuote.rfqId)
      } else {
        fullfiledPriceQuotes(priceQuoteFullFilled.priceQuote.rfqId) = currentFullfilement
      }
 
    case m =>
      log.warning(s"$self: receive unexpected message $m")
  }
}

// Application
class RecipientListSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "RecipientList" should {
    "determine the list of desired recipients, and forward the message" in {

      val probe = TestProbe()
      val priceQuoteAggregator = system.actorOf(Props[PriceQuoteAggregator], "priceQuoteAggregator")
      val orderProcessor = system.actorOf(
        Props(new MountaineeringSupplierOrderProcessor(priceQuoteAggregator)), "orderProcessor")

      system.actorOf(Props(new BudgetHikersPriceQuotes(orderProcessor)), "budget_hikers_price_quotes")
      system.actorOf(Props(new HighSierraPriceQuotes(orderProcessor)), "high_sierra_price_quotes")
      system.actorOf(Props(new MountainAscentPriceQuotes(orderProcessor)), "mountain_ascent_price_quotes")
      system.actorOf(Props(new PinnacleGearPriceQuotes(orderProcessor)), "pinnacle_gear_price_quotes")
      system.actorOf(Props(new RockBottomOuterwearPriceQuotes(orderProcessor)), "rock_bottom_outerwear_price_quotes")

      Thread.sleep(500)

      orderProcessor ! RequestForQuotation("123",
        Vector(RetailItem("1", 29.95),
          RetailItem("2", 99.5),
          RetailItem("3", 14.5)
        ))

      expectMsgType[QuotationFullfilement]

    }
  }
}
