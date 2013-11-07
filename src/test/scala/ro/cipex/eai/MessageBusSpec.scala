package ro.cipex.eai.message_bus

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

// Bus Protocol

case class CommandHandler(appId: String, handler: ActorRef)
case class ExecuteBuyOrder(portfolioId: String, symbol: String, quantity: Int, price: Double)
case class BuyOrderExecuted(portfolioId: String, symbol: String, quantity: Int, price: Double)
case class ExecuteSellOrder(portfolioId: String, symbol: String, quantity: Int, price: Double)
case class SellOrderExecuted(portfolioId: String, symbol: String, quantity: Int, price: Double)
case class NotificationInterest(appId: String, interested: ActorRef)
case class RegisterCommandHandler(appId: String, commandId: String, handler: ActorRef)
case class RegisterNotificationInterest(appId: String, notificationId: String, handler: ActorRef)
case class TradingCommand(commandId: String, command: Any)
case class TraidingNotification(notificationId: String, notification: Any)

case class Status

class MarketAnalysisTool(tradingBus: ActorRef) extends Actor with ActorLogging {

  tradingBus ! RegisterNotificationInterest(self.path.name, "BuyOrderExecuted", self)
  tradingBus ! RegisterNotificationInterest(self.path.name, "SellOrderEcecuted", self)

  def receive = LoggingReceive {
    case m =>
      log.info(s"$self: process analytics on $m")
  }
}

class PortfolioManager(tradingBus: ActorRef) extends Actor with ActorLogging {

  tradingBus ! RegisterNotificationInterest(self.path.name, "BuyOrderExecuted", self)
  tradingBus ! RegisterNotificationInterest(self.path.name, "SellOrderEcecuted", self)

  def receive = LoggingReceive {
    case m =>
      log.info(s"$self: adjust portfolio to $m")
  }
}

class StockTrader(tradingBus: ActorRef) extends Actor with ActorLogging {

  tradingBus ! RegisterCommandHandler(self.path.name, "ExecuteBuyOrder", self)
  tradingBus ! RegisterCommandHandler(self.path.name, "ExecuteSellOrder", self)

  def receive = LoggingReceive {
    case ExecuteBuyOrder(portfolioId, symbol, quantity, price) =>
      tradingBus ! TraidingNotification("BuyOrderExecuted", BuyOrderExecuted(portfolioId, symbol, quantity, price))
    case ExecuteSellOrder(portfolioId, symbol, quantity, price) =>
      tradingBus ! TraidingNotification("SellOrderExecuted", SellOrderExecuted(portfolioId, symbol, quantity, price))
    case m =>
      log.warning(s"$self: receive unexpected $m")
   }
}

class TraidingBus(s: Int) extends Actor with ActorLogging {
  val commandHandlers = mutable.Map[String, Vector[CommandHandler]]().withDefaultValue(Vector[CommandHandler]())
  val notificationInterest = mutable.Map[String, Vector[NotificationInterest]]().withDefaultValue(Vector[NotificationInterest]())

  var totalTegistered = 0

  def dispatchCommand(command: TradingCommand) =
    for (handlers <- commandHandlers.get(command.commandId);
      commandHandlers <- handlers) {
      commandHandlers.handler  ! command.command
    }

  def dispatchNotification(notification: TraidingNotification) =
    for (handlers <- notificationInterest.get(notification.notificationId);
      notificationInterest <- handlers
    ) {
      notificationInterest.interested ! notification.notification
    }

  def receive = LoggingReceive {
    case RegisterCommandHandler(appId, commandId, handler) =>
      commandHandlers(commandId) =
        commandHandlers(commandId) :+ CommandHandler(appId, handler)
    case RegisterNotificationInterest(appId, notificationId, handler) =>
      notificationInterest(notificationId) =
        notificationInterest(notificationId) :+ NotificationInterest(appId, handler)
    case command: TradingCommand =>
      dispatchCommand(command)
    case notification: TraidingNotification =>
      dispatchNotification(notification)
    case status: Status =>
      log.info(s"commands: ${commandHandlers.size}, notifications: ${notificationInterest.size}")
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}


// Application
class MessageBusSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "MessageBus" should {
    "run SOA" in {
      val tradingBus = system.actorOf(Props(new TraidingBus(6)), "tradingBus")
      val marketAnalysisTool = system.actorOf(Props(new MarketAnalysisTool(tradingBus)), "marketAnalysisTool")
      val portfolioManager = system.actorOf(Props(new PortfolioManager(tradingBus)), "portfolioManager")
      val stockTrader = system.actorOf(Props(new StockTrader(tradingBus)), "stockTrader")

      tradingBus ! Status()

      tradingBus ! TradingCommand("ExecuteBuyOrder", ExecuteBuyOrder("p123", "MSFT", 100, 31.85))
      tradingBus ! TradingCommand("ExecuteSellOrder", ExecuteSellOrder("p123", "MSFT", 100, 31.85))
    }
  }
}
