package ro.cipex.eai.domain

import scala.concurrent.Await
import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.Timeout
import akka.pattern.ask

case class AggregateType(worker: AggregateCacheWorker, actor: ActorRef)

case class AggregateRef(id: String, cache: ActorRef) {
  def tell(message: Any)(implicit sender: ActorRef = null): Unit = {
    cache ! CacheMessage(id, message, sender)
  }

  def !(message: Any)(implicit sender: ActorRef = null): Unit = {
    cache ! CacheMessage(id, message, sender)
  }
}

/**
  * DomainModel
  */
object DomainModel {
  def apply(name: String): DomainModel = {
    new DomainModel(name)
  }
}

case class ProvideWorker()

class DomainModel(name: String) {

  val aggregateTypeRegistry = scala.collection.mutable.Map[String, AggregateType]()
  val system = ActorSystem(name)

  def aggregateOf(typeName: String, props: Props, id: String) = {
    if (aggregateTypeRegistry.contains(typeName)) {
      val aggregateType = aggregateTypeRegistry(typeName)
      val worker = aggregateType.worker
      val actorRef = worker.aggregateOf(props, id)
      val cacheActor = aggregateType.actor
      AggregateRef(id, cacheActor)
    } else {
      AggregateRef(null, null)
    }
  }

  def registerAggregateType(typeName: String): Unit = {
    if (!aggregateTypeRegistry.contains(typeName)) {
      val actorRef = system.actorOf(Props(new AggregateCache(typeName)), typeName)      
      implicit val timeout = Timeout(5)      
      
      val future = actorRef ? ProvideWorker
      val worker = Await.result(future, timeout.duration).asInstanceOf[AggregateCacheWorker]
      aggregateTypeRegistry(typeName) = AggregateType(worker, actorRef)
    }
  }

  def shutdown() = {
    system.shutdown()
  }
}

case class CacheMessage(id: String, actualMessage: Any, sender: ActorRef)

class AggregateCache(typeName: String) extends Actor {
  val worker = new AggregateCacheWorker(context)

  def receive = {
    case message: CacheMessage =>
      val aggregate = context.child(message.id).getOrElse {
        context.actorOf(worker.propsFor(message.id), message.id)
      }
      aggregate.tell(message.actualMessage, message.sender)

    case ProvideWorker =>
      sender ! worker
  }
}

class AggregateCacheWorker(context: ActorContext) {
  val aggregateInfo = scala.collection.mutable.Map[String, Props]()

  def aggregateOf(props: Props, id: String): ActorRef = {
    if (!aggregateInfo.contains(id)) {
      aggregateInfo(id) = props
      context.actorOf(props, id)
    } else {
      throw new IllegalStateException(s"Aggregate with id $id already exists")
    }
  }

  def propsFor(id: String): Props = {
    if (aggregateInfo.contains(id)) {
      aggregateInfo(id)
    } else {
      throw new IllegalStateException(s"No Props for aggregate of id $id")
    }
  }
}










