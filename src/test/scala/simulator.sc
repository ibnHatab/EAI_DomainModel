
import scala.concurrent._

import ExecutionContext.Implicits.global


object simulator {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
  val a = 1                                       //> a  : Int = 1
   
  object Direction extends Enumeration {
    type Direction = Value
    val Left, Right, Up, Down = Value
  }
  
  import Direction._
  
  Direction(1)                                    //> res0: simulator.Direction.Value = Right
  Direction.maxId                                 //> res1: Int = 4
  
  def createSessionFor(user: String) = {
  	Thread.sleep(1000)
  	user.toUpperCase().toList
  }                                               //> createSessionFor: (user: String)List[Char]
  
    
  val session = future {
  	createSessionFor("corni")
  }                                               //> session  : scala.concurrent.Future[List[Char]] = scala.concurrent.impl.Promi
                                                  //| se$DefaultPromise@193b520

  val f = future {
    2 / 0
  }                                               //> f  : scala.concurrent.Future[Int] = scala.concurrent.impl.Promise$DefaultPro
                                                  //| mise@6901dc
  f onFailure {
    case npe: NullPointerException =>
      println("I'd be amazed if this printed out.")
  }
  
  val xs = List.tabulate(5)(_ + 1)                //> xs  : List[Int] = List(1, 2, 3, 4, 5)
	val ys = xs.view map { x => println(x); x * x }
                                                  //> ys  : scala.collection.SeqView[Int,Seq[_]] = SeqViewM(...)
	ys.head                                   //> 1
                                                  //| res2: Int = 1
	ys.force                                  //> 1
                                                  //| 2
                                                  //| 3
                                                  //| 4
                                                  //| 5
                                                  //| res3: Seq[Int] = List(1, 4, 9, 16, 25)
    Promise.successful("foo").future              //> res4: scala.concurrent.Future[String] = scala.concurrent.impl.Promise$KeptPr
                                                  //| omise@1d484c3
}