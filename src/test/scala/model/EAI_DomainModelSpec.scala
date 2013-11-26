package ro.cipex.eai.domain

import org.scalatest.WordSpecLike
import org.scalatest.Matchers

import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

object conf {

val customConf = ConfigFactory.parseString("""
		akka.loglevel = "DEBUG"
		akka.event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
		akka.actor.debug.autoreceive = on
		akka.actor.debug.lifecycle = on
		akka.actor.debug.receive = on
		akka.actor.debug.event-stream = on
		""")
}

case class ProcessOrder()

class Order(id: String, amount: Double) extends Actor {
	def receive = {
    case p: ProcessOrder =>
      println(s"Processing Order is $p")
      sender ! 'ok
    case a: Any =>
      println(s"message is $a")
      sender ! 'error
  }
}

// class EAI_DomainModelSpec extends TestKit(ActorSystem("DomainModel"// ,
//   // ConfigFactory.load(conf.customConf)
// )) with ImplicitSender with WordSpecLike with Matchers {
  
//   "DomainModel service" should {
//     "start and shutdown" in {
//       val model = DomainModel("prototype")

//       model.registerAggregateType("Order")

//       val order = model.aggregateOf("Order", Props(new Order("123", 249.95)), "123")

//       order ! ProcessOrder()
      
//       expectMsg('ok)
      
//       model.shutdown()

//       println("DomainModelPrototype: is completed.")
//     }
//   }
// }

