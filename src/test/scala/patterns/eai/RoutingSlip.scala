package ro.cipex.eai.pattern.routing_slip

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
case class CustomerInformation(val name: String, val federalTaxId: String)

case class PostalAddess(
  val address: String,
  val city: String,
  val zip: String)

case class Telephone(val number: String)

case class ContactInformation(
  val postalAddess: PostalAddess,
  val telephone: Telephone)

case class ServiceOption(val id: String, val desc: String)

case class RegistrationData(
  val customerInformation: CustomerInformation,
  val ContactInformatino: ContactInformation,
  val ServiceOption: ServiceOption)

// Machinery
case class ProcessStep(val name: String, val processor: ActorRef)

case class IllegelStepException(message: String) extends Exception (message)

case class RegistrationProcess(
  val processId: String,
  val processSteps: Seq[ProcessStep],
  val currentStep: Int = 0) {

  def isCompleted = currentStep >= processSteps.size

  def nextStep: ProcessStep = {
    if(isCompleted)
      throw new IllegelStepException("Process had already completed")
    else
      processSteps(currentStep)
  }

  def stepCompleted: RegistrationProcess =
    new RegistrationProcess(processId, processSteps, currentStep + 1)
}

case class RegisterCustomer(
  val registrationData: RegistrationData,
  registrationProcess: RegistrationProcess) {

  def addvance() = {
    val addvancedProcess = registrationProcess.stepCompleted
    if(! addvancedProcess.isCompleted)
      addvancedProcess.nextStep.processor ! RegisterCustomer(registrationData, addvancedProcess)
  }
}

// Business Logic 

class ContactKeeper extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case registerCustomer: RegisterCustomer =>
      val contactInfo = registerCustomer.registrationData.customerInformation
      log.debug(s"do save contacts $contactInfo")
      registerCustomer.addvance()
      context.stop(self)
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}
class CreditChecker extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case registerCustomer: RegisterCustomer =>
      val taxId = registerCustomer.registrationData.customerInformation.federalTaxId
      log.debug(s"do credit checker on: $taxId")
      registerCustomer.addvance()
      context.stop(self)
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}
class CustomerVault extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case registerCustomer: RegisterCustomer =>
        registerCustomer.addvance()
        context.stop(self)
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}
class ServicePlanner extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case registerCustomer: RegisterCustomer =>
        registerCustomer.addvance()
        context.stop(self)
    case m =>
      log.warning(s"$self: receive unexpected $m")
  }
}

object ServiceRegistry {
  def contactKeeper(system: ActorSystem, id: String) =
    system.actorOf(Props[ContactKeeper], "contactkeeper")

  def creditChecker(system: ActorSystem, id: String) =
    system.actorOf(Props[CreditChecker], "creditChecker")

  def customerVault(system: ActorSystem, id: String) =
    system.actorOf(Props[CustomerVault], "customerVault")

  def servicePlanner(system: ActorSystem, id: String) =
    system.actorOf(Props[ServicePlanner], "servicePlanner")

}

// Application
class RoutingSlipSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "Routing Slip" should {
    "specifying the sequence of processing steps" in {
      val processId = java.util.UUID.randomUUID().toString

      val step1 = ProcessStep("create_customer", ServiceRegistry.customerVault(system, processId))
      val step2 = ProcessStep("create_customer", ServiceRegistry.contactKeeper(system, processId))
      val step3 = ProcessStep("create_customer", ServiceRegistry.servicePlanner(system, processId))
      val step4 = ProcessStep("create_customer", ServiceRegistry.creditChecker(system, processId))

      val registrationProcess = new RegistrationProcess(processId, Vector(step1, step2, step3, step4))

      val registrationData =
        new RegistrationData(
          CustomerInformation("ABC Inc.", "80301"),
          ContactInformation(
            PostalAddess("123 Main Stret", "NY", "1234"),
            Telephone("303-555-1212")),
          ServiceOption("99-1203", "A description of 99-1203")
        )

      val registerCustomer = RegisterCustomer(registrationData, registrationProcess)

      registrationProcess.nextStep.processor ! registerCustomer


    }
  }
}
