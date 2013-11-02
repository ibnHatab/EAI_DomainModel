package ro.cipex.eai.content_enricher

import scala.language.postfixOps

import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef, ActorLogging, Props }
import akka.event.LoggingReceive

import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe

import java.util.Date

case class CompleteVisit(dispatcher: ActorRef) 

case class PatientDetails(val lastName: String, val socialSecurityNumber: String, val carrier: String) 

case class DoctorVisitCompleted(val patientId: String, val firstName: String, date: Date) {
  var patientDetails: PatientDetails = _

  def carrier = patientDetails.carrier

  def enrichWith(patientDetails: PatientDetails) {
    this.patientDetails = patientDetails
  }

  def lastName = patientDetails.lastName

  def socialSecurityNumber = patientDetails.socialSecurityNumber
}

class AccountEnricherDispatcher(accountSystemDispatcher: ActorRef) extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case doctorVisitCompleted: DoctorVisitCompleted =>
      log.debug("AccountEnricher: query and forward")
      val (lastName, carrier, socialSecurityNumber) = ("Doe", "Kaiser", "111-22-3333")

      doctorVisitCompleted.enrichWith(PatientDetails(lastName, socialSecurityNumber, carrier))
      accountSystemDispatcher forward doctorVisitCompleted
    case _ =>
      log.warning("AccountEnricher: receive unexpected message")
  }
}

class AccountSystemDispatcher(reception: ActorRef) extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case doctorVisitCompleted: DoctorVisitCompleted =>
      reception ! doctorVisitCompleted
    case _ =>
      log.warning("AccountDispatcher: receive unexpected message")
  }
}

class SheduledDoctorVisit(val patientId: String, val firstName: String) extends Actor with ActorLogging {
  var completedOn: Date = _

  def receive = LoggingReceive {
    case visitCompleted: CompleteVisit =>
      completedOn = new Date
      visitCompleted.dispatcher ! DoctorVisitCompleted(patientId, firstName, completedOn)
    case _ =>
      log.warning("SheduledDoctorVisit: receive unexpected message")
  }
}

// Application
class ContentEnricherSpec extends TestKit(ActorSystem("EAI"))
    with ImplicitSender with WordSpecLike with Matchers {
  
  "ContentEnricher" should {
    "augment a message with missing information" in {
      val reception = TestProbe()

      val accountSystemDispatcher =
        system.actorOf(Props(
          new AccountSystemDispatcher(reception.ref)), "accountSystem")

      val accountEnricherDispatcher =
        system.actorOf(Props(
          new AccountEnricherDispatcher(accountSystemDispatcher)), "accountEnricher")

      val scheduledDoctorVisit = system.actorOf(Props(
        new SheduledDoctorVisit("123456789", "John")), "sheduledVisit")

      scheduledDoctorVisit ! CompleteVisit(accountEnricherDispatcher)

      val confirmation = reception.expectMsgType[DoctorVisitCompleted]

      confirmation.lastName should equal("Doe")

    }
  }
}
