package patterns.eai.persistenceevent_sourcing_example

import scala.concurrent._
import scala.concurrent.duration._

import akka.pattern.ask
import akka.util.Timeout

import akka.actor.{ Actor, Props, ActorSystem, ActorLogging }
import akka.event.LoggingReceive

import akka.persistence._


case class Cmd(data: String)
case class Evt(data: String)

case class ExState(events: List[String] = Nil) {
  def update(evt: Evt) = copy(evt.data :: events)
  def size = events.size
  override def toString = events.reverse.toString
}

class ExProcessor extends EventsourcedProcessor with ActorLogging {
  val state = ExState()
 
  def update(evt: Evt) = state.update(evt)

  def numEvents = state.size

  def receiveCommand: PartialFunction[Any,Unit] = { case _ => () }
  def receiveReplay: PartialFunction[Any,Unit] = { case _ => () }


}

object EventSourcingExemple extends App {

  val system = ActorSystem("example")
  import system.dispatcher

  system.log.info(">> START")

  Thread.sleep(1000)
  system.shutdown()
}

