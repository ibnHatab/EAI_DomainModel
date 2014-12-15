package ro.cipex.eai.domain.immutable

import scala.language.postfixOps
import scala.collection.immutable

import scala.language.implicitConversions
import scala.language.reflectiveCalls

import org.scalatest.WordSpecLike
import org.scalatest.Matchers

object Pipelining {
  implicit def toPipe[T](x : T) = new {
    def |> [U](f : T => U) = f(x)
  }
}

import Pipelining._

case class Employee(id: Int = -1, val name: String, val salary: Double) {
  def UpdateSalary(update: Double => Double) =
    this.copy(salary = update(salary))
}

case class Company (
  name: String,
  employees: immutable.Map[Int, Employee] = immutable.Map[Int, Employee](),
  autoid: Int = 1
) {
  self  =>
 
  def AddEmployee(employee: Employee) = {
    store_employee(employee.copy(id = self.autoid)).
      update_autoid { id => id + 1 }
  }

  def GetEmployee(id: Int) = employees.get(id)

  def UpdateEmployee(id: Int, update: Employee => Employee) = {
    GetEmployee(id) |> maybe_update_employee(update)
  }

  
  private def store_employee(employee: Employee) = {
    update_employees( dict => dict + ((employee.id, employee)) )
  }

  private def update_employees(update: immutable.Map[Int, Employee] => immutable.Map[Int, Employee]) =
    this.copy(employees = update(employees))

  private def update_autoid(update: Int => Int) = this.copy(autoid = update(autoid))

  private def maybe_update_employee(update: Employee => Employee)(employee: Option[Employee]) =
    employee match {
      case None => None
      case Some(e) =>
        update(e) |> store_employee
    }
}

object Company {
  def Factory(name: String, input: List[(String, Double)]) =
    input.foldLeft (Company(name)) { case (company, data) =>
      company.AddEmployee(Employee(name = data._1, salary = data._2))
    }
    
}

class ImmutableObjectSpec extends WordSpecLike with Matchers {

  "Immutable Objct" should {
    "CRUD imutably" in {
      val c = Company(name = "Intech").
        AddEmployee(Employee(name = "Peter Gibbons", salary = 10000)).
        AddEmployee(Employee(name = "Michael Bolton", salary = 20000))

      val e1 = c.GetEmployee(1).get

      val c1 = c.UpdateEmployee(1, { case employee =>
        employee.UpdateSalary { s => s * 1.2 }})

      e1.salary should equal (10000)
    }

    "support for Repository" in {
      val input = List(
        ("Peter Gibbons", 10000.0),
        ("Michael Bolton", 20000.0)
      )

      val c = Company.Factory("Intech", input)
      c.GetEmployee(1).get.name should equal("Peter Gibbons")
    }
  }
}

