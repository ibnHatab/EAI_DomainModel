import scala.language.postfixOps
import scala.util._
import scala.util.control.NonFatal
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
//import scala.async.Async.{async, await}


object futureWS {

trait Socket {
	def readFromMemory(): Future[Array[Byte]]
	def sendToEurope(packet: Array[Byte]): Future[Array[Byte]]
}

import ExecutionContext.Implicits.global
val socket = new Socket {
	def readFromMemory (): Future[Array[Byte]] = Future {
		println(">> read")
	 	Array[Byte](1,2,3,4)
	}
	
	def sendToEurope(packet: Array[Byte]): Future[Array[Byte]] = Future {
		println(">> send")
	 	Array[Byte](1,2)
	}
}                                                 //> socket  : futureWS.Socket = futureWS$$anonfun$main$1$$anon$1@d5631c

val confirmation = for {
	packet <- socket.readFromMemory()
  confirmation <- socket.sendToEurope(packet)
} yield confirmation                              //> >> read
                                                  //| confirmation  : scala.concurrent.Future[Array[Byte]] = scala.concurrent.impl
                                                  //| .Promise$DefaultPromise@450ea8

confirmation.onSuccess{ case v =>
 	println(v)
 }                                                //> >> send
 
 println("END")                                   //> END
}