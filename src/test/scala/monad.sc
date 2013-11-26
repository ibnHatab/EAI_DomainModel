
import scala.util.{Try, Success, Failure}

object monad {

  case class LG[T] (val session: T) {
  	def flatMap[S] (f: T => LG[S]): LG[S] = f(session)
  	def map[S] (f: T => S): LG[S] = LG(f(session))
  }

  type SimpleLG = LG[String]

  def auth(a: String): SimpleLG = ???
  def readFeed(a: String): SimpleLG = ???
  def closeFeed(a: String): SimpleLG = ???

  def reading = for {
  	a <- auth("me")
  	b <- readFeed(a)
  	c <- closeFeed(b)
  } yield c

}
