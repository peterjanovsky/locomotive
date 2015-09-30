package com.pjanof.stores

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import com.pjanof.results.CResults._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.UUID
import java.net.URI

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import spray.json._

import scalaz._
import Scalaz._

@DoNotDiscover
class CouchbaseSpec extends FlatSpec with Matchers with ScalaFutures with DefaultJsonProtocol {

  val processors: Int = Runtime.getRuntime.availableProcessors
  val es: ExecutorService = Executors.newFixedThreadPool(processors)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(es)

  val config: Config = ConfigFactory.load

  val uris: List[URI] = config.getStringList("couchbase.bucket.uris").asScala.map( uri => new URI(uri) ).toList

  val cb: Couchbase = Couchbase(
    uris
    , config.getString("couchbase.bucket.name")
    , config.getString("couchbase.bucket.username")
    , config.getString("couchbase.bucket.password")
    , config.getInt("couchbase.bucket.timeoutInSeconds") )

  case class Test(a: String, b: Int)

  implicit val testFormat = jsonFormat2(Test.apply)

  case class Test2(a: String, b: Int, c: String)

  implicit val test2Format = jsonFormat3(Test2.apply)

  def randUUID = UUID.randomUUID

  val docKey: UUID = randUUID

  val test = Test("foo", 1)

  "The Couchbase interface" should "create a new document" in {

    val f: Future[CResult[Test]] = cb.save(docKey.toString, test)
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

    val result: CResult[Test] = cb.byKey[Test](docKey.toString)
    result match {
      case -\/(e) => fail(e.msg)
      case \/-(s) => assert(s == test)
    }
  }

  it should "update the previously created document using a Map" in {

    val m: Map[String,String] = Map( "a" -> "bar" )

    val f: Future[CResult[Test]] = cb.update[Test, Map[String,String], Test](docKey.toString, m) {
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

    val f: Future[CResult[Test]] = cb.update[Test,Test,Test](docKey.toString, updated) {
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

    val f: Future[CResult[Test2]] = cb.update[Test,Test2,Test2](docKey.toString, test2) {
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

  // currently fails as the document is not available within the view
  ignore should "return the previously created document when queried through a view" in {

    val result: CResult[List[Test2]] =
      cb.query[Test2](  config.getString("couchbase.testView.designDocumentName")
                      , config.getString("couchbase.testView.viewName")
                      , List("foobarbaz", docKey.toString) )

    result match {
      case -\/(e) => fail(e.msg)
      case \/-(s) => s should contain only (Test2("foobarbaz", 7, "foofoo"))
    }
  }

  it should "delete the previously created document" in {

    val f: Future[CResult[Boolean]] = cb.delete(docKey.toString)
    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) => assert(s == true)
      }
    }
  }

  it should "create a new document with an expiry" in {

    val expiry: Int = 1
    val expiryKey: UUID = randUUID

    val f: Future[CResult[Test]] = cb.save(expiryKey.toString, test, expiry, cb.Master)
    whenReady(f) { result =>
      result match {
        case -\/(e) => fail(e.msg)
        case \/-(s) =>
          assert(s.a == test.a)
          assert(s.b == test.b)
      }
    }

    val result: CResult[Test] = cb.byKey[Test](expiryKey.toString)
    result match {
      case -\/(e) => fail(e.msg)
      case \/-(s) => assert(s == test)
    }

    Thread.sleep(5000)

    val result2: CResult[Test] = cb.byKey[Test](expiryKey.toString)
    result2 match {
      case -\/(e) => assert(e.isInstanceOf[UnmarshalException])
      case \/-(s) => fail("Document Expiry Failure")
    }
  }
}
