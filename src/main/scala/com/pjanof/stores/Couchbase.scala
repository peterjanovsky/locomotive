package com.pjanof.stores

import com.pjanof.results.CResults._
import com.typesafe.scalalogging.Logger
import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views.ComplexKey
import com.couchbase.client.protocol.views.View
import com.couchbase.client.protocol.views.Query
import com.couchbase.client.protocol.views.ViewResponse
import com.couchbase.client.protocol.views.ViewRow

import net.spy.memcached.CachedData
import net.spy.memcached.CASResponse
import net.spy.memcached.CASValue
import net.spy.memcached.internal.OperationFuture
import net.spy.memcached.internal.OperationCompletionListener
import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.PersistTo

import org.slf4j.LoggerFactory

import spray.json.{DeserializationException, JsonParser, JsValue, ParserInput, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.concurrent.duration.SECONDS
import scala.concurrent.ExecutionContext
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.Try

import java.net.URI

import scalaz._
import Scalaz._

class Couchbase private(uris: List[URI], bucket: String, username: String, password: String, timeout: Long) extends DocumentStore {

  private val logger: Logger = Logger(LoggerFactory.getLogger(getClass))

  val fromPersistenceRule: PersistenceRuleTransformer[PersistTo] = _ match {
    case Master => PersistTo.MASTER
    case AtLeastOneNode => PersistTo.ONE
  }

  private val client: CouchbaseClient = new CouchbaseClient(uris.asJava, bucket, username, password)

  def close()(implicit ec: ExecutionContext): Future[CResult[Boolean]] = Future(client.shutdown(timeout, SECONDS).s)

  def save[A](key: String, a: A)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[CResult[A]] = {

    val json: String = fmt.write(a).toString

    val opFuture: OperationFuture[java.lang.Boolean] = client.set(key, json)
    val transformer: OperationTransformerF[java.lang.Boolean] = new OperationTransformerF[java.lang.Boolean](opFuture)

    val f: Future[java.lang.Boolean] = transformer.future
    f.map { res => if (res) {

        logger.debug(s"Wrote Document for Key: $key with $json")
        a.s

      } else {

        logger.error(s"Unable to Write Document for Key: $key with $json")
        StoreException("Write Failure").f

      } }
  }

  def save[A](key: String, a: A, exp: Int, rule: PersistenceRule)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[CResult[A]] = {

    val json: String = fmt.write(a).toString

    val opFuture: OperationFuture[java.lang.Boolean] = client.set(key, exp, json, fromPersistenceRule(rule))
    val transformer: OperationTransformerF[java.lang.Boolean] = new OperationTransformerF[java.lang.Boolean](opFuture)

    val f: Future[java.lang.Boolean] = transformer.future
    f.map { res => if (res) {

        logger.debug(s"Wrote Document for Key: $key with $json")
        a.s

      } else {

        logger.error(s"Unable to Write Document for Key: $key with $json")
        StoreException("Write Failure").f

      } }
  }

  def byKey[A](key: String)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[A] = {

    val json: JsValue = client.get(key, new JsonTranscoder)

    logger.debug(s"Retrieved Document by Key: $key as ${json.compactPrint}")

    try { fmt.read(json).s }
    catch {
      case e: DeserializationException =>
        logger.error(s"Unable to Unmarshal JSON: ${json.compactPrint} with ${e.getMessage}")
        UnmarshalException(s"Unmarshal Failure: ${e.getMessage}").f
    }
  }

  def update[A,B,C](key: String, b: B)(f1: (A,B) => C)(implicit fmt1: RootJsonFormat[A], fmt2: RootJsonFormat[C], ec: ExecutionContext): Future[CResult[C]] = {

    val cv: CASValue[JsValue] = client.gets(key, new JsonTranscoder)
    if (cv == null) {

      logger.info(s"Attempted to Update Invalid Document for Key: $key with $b")
      Future(StoreException("Document Not Found").f)

    } else {

      try {

        val a: A = fmt1.read(cv.getValue)

        val merged: C = f1(a,b)
        val json = fmt2.write(merged)

        val opFuture: OperationFuture[CASResponse] = client.asyncCAS(key, cv.getCas, 0, json, new JsonTranscoder)
        val transformer: OperationTransformerF[CASResponse] = new OperationTransformerF[CASResponse](opFuture)

        val f: Future[CASResponse] = transformer.future
        f.map(res => res match {

          case CASResponse.EXISTS =>

            logger.error(s"Unable to Update Document, CAS Value Changed for Key: $key with $json")
            StoreException("Document Previously Updated").f

          case CASResponse.NOT_FOUND =>

            logger.error(s"Document Not Found for Key: $key with $json")
            StoreException("Document Not Found").f

          case CASResponse.OBSERVE_ERROR_IN_ARGS =>

            logger.error(s"Invalid Document Arguments for Key: $key with $json")
            StoreException("Invalid Document Arguments").f

          case CASResponse.OBSERVE_MODIFIED =>  // NOT necessarily an exception, surface within userland?

            logger.info(s"Document Updated and Modified within Observe for Key: $key with $json")
            merged.s

          case CASResponse.OBSERVE_TIMEOUT =>

            logger.error(s"Unable to Validate Document Update due to Observe Timeout for Key: $key with $json")
            StoreException("Validation Timeout").f

          case CASResponse.OK =>

            logger.debug(s"Updated Document for Key: $key with $json")
            merged.s
        })

      } catch {

        case e: DeserializationException =>

          logger.error(s"Error Parsing Document: ${e.getMessage}")
          Future(UnmarshalException(s"Unable to Unmarshal Document: ${e.getMessage}").f)
      }
    }
  }

  def delete(key: String)(implicit ec: ExecutionContext): Future[CResult[Boolean]] = {

    val opFuture: OperationFuture[java.lang.Boolean] = client.delete(key)
    val transformer: OperationTransformerF[java.lang.Boolean] = new OperationTransformerF[java.lang.Boolean](opFuture)

    val f: Future[java.lang.Boolean] = transformer.future
    f.map(res => if (res) {

        logger.debug(s"Document Marked to Expire for Key: $key")
        true.s

      } else {

        logger.info(s"Attempt to Expire Invalid Document for Key: $key")
        false.s

      })
  }

  def query[A](designDocumentName: String, viewName: String, keyParts: List[Object])(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[List[A]] = {

    val view: View = client.getView(designDocumentName, viewName)

    val key: ComplexKey = ComplexKey.of(keyParts:_*)

    val query: Query = new Query
    query.setKey(key)

    val response: ViewResponse = client.query(view, query)

    val rows: List[ViewRow] = response.iterator.asScala.toList
    val res: List[CResult[A]] = rows.map( row =>

      try { fmt.read( JsonParser(ParserInput(row.getValue)) ).s }
      catch {

        case e: DeserializationException =>

          logger.error(s"Error Parsing Document: ${e.getMessage}")
          UnmarshalException(s"Unable to Unmarshal Document: ${e.getMessage}").f
      } )

    res.sequence
  }

  private class OperationTransformerF[T](opFuture: OperationFuture[T]) extends OperationCompletionListener {

    private val promise = Promise[T]

    def future = promise.future

    opFuture.addListener(this)

    def onComplete(f: OperationFuture[_]): Unit =
      promise.complete(Try(opFuture.get))
  }

  private class JsonTranscoder extends Transcoder[JsValue] {

    def decode(cd: CachedData): JsValue = JsonParser(ParserInput(cd.getData))

    def encode(json: JsValue): CachedData =
      new CachedData(0, json.toString getBytes, CachedData.MAX_SIZE)

    def getMaxSize: Int = CachedData.MAX_SIZE

    def asyncDecode(cd: CachedData): Boolean = false
  }
}

object Couchbase {

  def apply(uris: List[URI], bucket: String, username: String, password: String, timeout: Long): Couchbase =
    new Couchbase(uris, bucket, username, password, timeout)
}
