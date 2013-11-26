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
  def update_salary(update: Double => Double) =
    this.copy(salary = update(salary))
}

case class Company (
  name: String,
  employees: immutable.Map[Int, Employee] = immutable.Map[Int, Employee](),
  autoid: Int = 1
) {
  self  =>
  

  def add_employee(employee: Employee) = {
    store_employee(employee.copy(id = self.autoid)).
      update_autoid { id => id + 1 }
  }

  def store_employee(employee: Employee) = {
    update_employees( dict => dict + ((employee.id, employee)) )
  }

  def update_employees(update: immutable.Map[Int, Employee] => immutable.Map[Int, Employee]) =
    this.copy(employees = update(employees))

  def update_autoid(update: Int => Int) = this.copy(autoid = update(autoid))
  def get_employee(id: Int) = employees.get(id)

  def update_employee(id: Int, update: Employee => Employee) = {
    get_employee(id) |> maybe_update_employee(update)
  }

  def maybe_update_employee(update: Employee => Employee)(employee: Option[Employee]) =
    employee match {
      case None => None
      case Some(e) =>
        update(e) |> store_employee
    }
}

object Company {
  def factory(name: String, input: List[(String, Double)]) =
    input.foldLeft (Company(name)) { case (company, data) =>
      company.add_employee(Employee(name = data._1, salary = data._2))
    }
    
}

class ImmutableObjectSpec extends WordSpecLike with Matchers {

  "Immutable Objct" should {
    "CRUD imutably" in {
      val c = Company(name = "Intech").
        add_employee(Employee(name = "Peter Gibbons", salary = 10000)).
        add_employee(Employee(name = "Michael Bolton", salary = 20000))

      val e1 = c.get_employee(1).get

      val c1 = c.update_employee(1, { case employee =>
        employee.update_salary { s => s * 1.2 }})

      e1.salary should equal (10000)
    }

    "support for Repository" in {
      val input = List(
        ("Peter Gibbons", 10000.0),
        ("Michael Bolton", 20000.0)
      )

      val c = Company.factory("Intech", input)
      c.get_employee(1).get.name should equal("Peter Gibbons")
    }
  }
}

