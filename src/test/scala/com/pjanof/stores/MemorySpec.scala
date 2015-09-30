package com.pjanof.stores

import com.pjanof.results.CResults._

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.UUID

import scalaz._
import Scalaz._

class MemorySpec extends FlatSpec with Matchers with ScalaFutures with DefaultJsonProtocol {

  val processors: Int = Runtime.getRuntime.availableProcessors
  val es: ExecutorService = Executors.newFixedThreadPool(processors)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(es)

  val memory = Memory()

  case class Test(a: String, b: Int)

  implicit val testFormat = jsonFormat2(Test.apply)

  case class Test2(a: String, b: Int, c: String)

  implicit val test2Format = jsonFormat3(Test2.apply)

  def randUUID = UUID.randomUUID

  val docKey: UUID = randUUID

  val test = Test("foo", 1)

  "The Memory interface" should "create a new document" in {

    val f: Future[CResult[Test]] = memory.save(docKey.toString, test)
    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) =>
          assert(s.a == test.a)
          assert(s.b == test.b)
      }
    }
  }

  it should "return the previously created document when queried by key" in {

    val result: CResult[Test] = memory.byKey[Test](docKey.toString)
    result match {
      case -\/(e) => fail(e.msg)
      case \/-(s) => assert(s == test)
    }
  }

  it should "update the previously created document using a Map" in {

    val m: Map[String,String] = Map( "a" -> "bar" )

    val f: Future[CResult[Test]] = memory.update[Test, Map[String,String], Test](docKey.toString, m) {
      (x,y) => x.copy( a = y.getOrElse("a","defaultValue") )
    }

    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) =>
          assert(s.a == "bar")
          assert(s.b == 1)
      }
    }
  }

  it should "update the previously created document using a Case Class" in {

    val updated = Test("baz", 3)

    val f: Future[CResult[Test]] = memory.update[Test,Test,Test](docKey.toString, updated) {
      (x,y) => x.copy( a = y.a, b = y.b )
    }

    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) => assert(s == updated)
      }
    }
  }

  it should "update the previously created document morphing it's shape" in {

    val test2 = Test2("foobar", 4, "foofoo")

    val f: Future[CResult[Test2]] = memory.update[Test,Test2,Test2](docKey.toString, test2) {
      (x,y) => y.copy( a = y.a + x.a, b = y.b + x.b )
    }

    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) =>
          assert(s.a == "foobarbaz")
          assert(s.b == 7)
          assert(s.c == "foofoo")
      }
    }
  }

  it should "delete the previously created document" in {

    val f: Future[CResult[Boolean]] = memory.delete(docKey.toString)
    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) => assert(s == true)
      }
    }
  }
}
