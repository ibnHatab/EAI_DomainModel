package ro.cipex.eai.scatter_gather

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
case class RequestForQuotation(rfqId: String, retailItems: Seq[RetailItem]) {
  def totalRetailPrice = retailItems.foldLeft(0.0) (_ + _.retailPrice)
}

case class RetailItem(itemId: String, retailPrice: Double)

case class RequestPriceQuote(rfqId: String, itemId: String, retailPrice: Double, orderTotalRetailPrice: Double)

case class PriceQuote(rfqId: String, itemId:String, retailPrice: Double, discountPrice: Double)

case class PriceQuoteFullFilled(priceQuote: PriceQuote)

case class PriceQuoteTimeout(rfqId: String)

case class RequiredPriceQuoteForFullfillement(rfqId: String, quotqRequested: Int)

case class BestPriceQuotation(rfqId: String, priceQuotes: Seq[PriceQuote])

case class SubscriBeToPriceQuoteRequest(quoterId: String, quoteProcessor: ActorRef)

// Processors
abstract class RetailPriceQuotes(
  orderProcessor: ActorRef,
  retailPrice: Double,
  discountPrice: Double) extends Actor with ActorLogging {

  def discountPercentage(orderedTotalRetailPrice: Double): Double

  override def preStart(): Unit = {
    orderProcessor ! SubscriBeToPriceQuoteRequest(self.path.toString, self)
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

class MountaineeringSupplierOrderProcessor(priceQuoteAggregator: ActorRef) extends Actor with ActorLogging {
  val subscribers = mutable.Map[String, SubscriBeToPriceQuoteRequest]()    
  val clientRegister = mutable.Map[String, ActorRef]()

  def dispatch(rfq: RequestForQuotation) = {
    subscribers.values.map { subscriber =>
      val quoteProcessor = subscriber.quoteProcessor
      rfq.retailItems.map { retailItem =>
        quoteProcessor ! RequestPriceQuote(rfq.rfqId, retailItem.itemId, retailItem.retailPrice, rfq.totalRetailPrice)
      }
    }
  }

  def receive = LoggingReceive {
    case subscription: SubscriBeToPriceQuoteRequest =>
      subscribers(subscription.quoterId) = subscription

    case priceQuote: PriceQuote =>
      priceQuoteAggregator ! PriceQuoteFullFilled(priceQuote)

    case rfq: RequestForQuotation  =>
      clientRegister(rfq.rfqId) = sender
      priceQuoteAggregator ! RequiredPriceQuoteForFullfillement(rfq.rfqId, subscribers.size * rfq.retailItems.size)
      dispatch(rfq)

      case bestPriceQuotation: BestPriceQuotation =>
      clientRegister(bestPriceQuotation.rfqId) ! bestPriceQuotation
      clientRegister.remove(bestPriceQuotation.rfqId)

    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}

case class QuotationFullfilement(rfqId: String, quoteRequsted: Int, priceQuotes: Seq[PriceQuote], requeter: ActorRef)

class PriceQuoteAggregator extends Actor with ActorLogging {
  val fullfiledPriceQuotes = mutable.Map[String, QuotationFullfilement]()

  import context.dispatcher

  def bestPriceQuotationFrom(quotationFullfilement: QuotationFullfilement): BestPriceQuotation = {
    val bestPrice = quotationFullfilement.priceQuotes.groupBy(_.itemId) map { case (w, ws) => (w, ws.maxBy(_.discountPrice))}
    BestPriceQuotation(quotationFullfilement.rfqId, bestPrice.values.toVector)
  }

  def receive = LoggingReceive {
    case required: RequiredPriceQuoteForFullfillement =>
      fullfiledPriceQuotes(required.rfqId) = QuotationFullfilement(required.rfqId, required.quotqRequested, Vector(), sender)
      context.system.scheduler.scheduleOnce (2 seconds, self, PriceQuoteTimeout(required.rfqId))

    case priceQuoteFullFilled: PriceQuoteFullFilled =>
      priceQuoteRequestFullFilled(priceQuoteFullFilled)

    case timeout: PriceQuoteTimeout =>
      priceQuoteTimeout(timeout.rfqId)
    case m =>
      log.warning(s"$self: receive unexpected message $m")
  }

  def priceQuoteTimeout(rfqId: String) = {
    if(fullfiledPriceQuotes.contains(rfqId)) 
      quoteBestPrice(fullfiledPriceQuotes(rfqId))
  }

  def  priceQuoteRequestFullFilled(priceQuoteFullFilled: PriceQuoteFullFilled) = {
    if(fullfiledPriceQuotes.contains(priceQuoteFullFilled.priceQuote.rfqId)) {
      val previoseFullfilement = fullfiledPriceQuotes(priceQuoteFullFilled.priceQuote.rfqId)
      val currentPriceQuoted = previoseFullfilement.priceQuotes :+ priceQuoteFullFilled.priceQuote
      val currentFullfilement = previoseFullfilement.copy(priceQuotes = currentPriceQuoted)
      if(currentPriceQuoted.size >= currentFullfilement.quoteRequsted) {
        quoteBestPrice(currentFullfilement)
      } else {
        fullfiledPriceQuotes(priceQuoteFullFilled.priceQuote.rfqId) = currentFullfilement
      }
    }
  }

  def quoteBestPrice(fullfilement: QuotationFullfilement) = {
    if(fullfiledPriceQuotes.contains(fullfilement.rfqId)) {
      fullfilement.requeter ! bestPriceQuotationFrom(fullfilement)
      fullfiledPriceQuotes.remove(fullfilement.rfqId)
    }
  }
}


// Application
class ScatterGatherSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ScatterGather" should {
    val priceQuoteAggregator = system.actorOf(Props[PriceQuoteAggregator], "priceQuoteAggregator")
    val orderProcessor = system.actorOf(
      Props(new MountaineeringSupplierOrderProcessor(priceQuoteAggregator)), "orderProcessor")

    "broadcasts a message to multiple recipients and re-aggregates the responses back" in {

      system.actorOf(Props(new BudgetHikersPriceQuotes(orderProcessor)), "budget_hikers_price_quotes")
      system.actorOf(Props(new HighSierraPriceQuotes(orderProcessor)), "high_sierra_price_quotes")
      system.actorOf(Props(new MountainAscentPriceQuotes(orderProcessor)), "mountain_ascent_price_quotes")
      system.actorOf(Props(new PinnacleGearPriceQuotes(orderProcessor)), "pinnacle_gear_price_quotes")
      system.actorOf(Props(new RockBottomOuterwearPriceQuotes(orderProcessor)), "rock_bottom_outerwear_price_quotes")

      orderProcessor ! RequestForQuotation("123",
        Vector(RetailItem("1", 29.95),
          RetailItem("2", 99.5),
          RetailItem("3", 14.5)
        ))

      expectMsgType[BestPriceQuotation]
    }
  }
}
