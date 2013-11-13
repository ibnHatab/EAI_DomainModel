
import java.util.Random

object console {
	
	val f: PartialFunction[Int, Any] = { case _ => 1/0 }
                                                  //> f  : PartialFunction[Int,Any] = <function1>

 	val g: PartialFunction[String, String] = { case "ping" => "pong" }
                                                  //> g  : PartialFunction[String,String] = <function1>
 	g.isDefinedAt("abc")                      //> res0: Boolean = false

 
 	trait Generator[+T] {
 		self =>
 		
 		def generate: T
 		
 		def map[S](f: T => S): Generator[S] = new Generator[S] {
 			def generate = f(self.generate)
 		}
 			
 	  def flatMap[S](f: T => Generator[S]): Generator[S] = new Generator[S] {
 			def generate = f(self.generate).generate
 		}
 	  	
 	}


	val integers = new Generator[Int] {
	 	val r = new Random()
 	
 		def generate = r.nextInt(10)
	}                                         //> integers  : console.Generator[Int]{val r: java.util.Random} = console$$anonf
                                                  //| un$main$1$$anon$3@1988175
	integers.generate                         //> res1: Int = 5
	
	val booleans = for { i<- integers } yield i > 5
                                                  //> booleans  : console.Generator[Boolean] = console$$anonfun$main$1$Generator$1
                                                  //| $$anon$1@650be6
	booleans.generate                         //> res2: Boolean = false
	
	def pairs[T, S](t: Generator[T], s: Generator[S]) = t.flatMap {
		x => s.map { y => (x, y) }
	}                                         //> pairs: [T, S](t: console.Generator[T], s: console.Generator[S])console.Gener
                                                  //| ator[(T, S)]
	
	def pairs1[T, S](t: Generator[T], s: Generator[S]) = for {
		a <- t
		b <- s
	} yield (a, b)                            //> pairs1: [T, S](t: console.Generator[T], s: console.Generator[S])console.Gene
                                                  //| rator[(T, S)]
	
	val pairs_val = pairs(integers, booleans) //> pairs_val  : console.Generator[(Int, Boolean)] = console$$anonfun$main$1$Gen
                                                  //| erator$1$$anon$2@1ef94a4
	
	pairs_val.generate                        //> res3: (Int, Boolean) = (5,false)
		
	def single[T](x: T) = new Generator[T] {
		def generate = x
	}                                         //> single: [T](x: T)console.Generator[T]
	
	def choose(lo: Int, hi: Int) = for { x <- integers } yield lo + x % (hi - lo)
                                                  //> choose: (lo: Int, hi: Int)console.Generator[Int]
	
	def oneOf[T](list: T*) = for { idx <- choose(0, list.length) } yield list(idx)
                                                  //> oneOf: [T](list: T*)console.Generator[T]
                                                  
	oneOf(1,2,3,4,5).generate                 //> res4: Int = 2
	
	def lists: Generator[List[Int]] = for {
		isEmpty <- booleans
		list <- if (isEmpty) emptyList else nonEmptyList
	} yield list                              //> lists: => console.Generator[List[Int]]
	
	def emptyList = single(Nil)               //> emptyList: => console.Generator[scala.collection.immutable.Nil.type]
	
	def nonEmptyList = for {
		x <- integers
		xs <- lists
	} yield x :: xs                           //> nonEmptyList: => console.Generator[List[Int]]
	
	
	lists.generate                            //> res5: List[Int] = List(2, 3, 2)
	
}