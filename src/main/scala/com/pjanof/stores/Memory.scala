package com.pjanof.stores

import com.pjanof.results.CResults._

import spray.json._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.typesafe.scalalogging.StrictLogging

/** DO NOT USE IN PRODUCTION, INTENDED FOR TESTING ONLY
  */
class Memory private() extends DocumentStore with StrictLogging {

  val tmap: TrieMap[String, String] = new TrieMap[String, String]()

  def close()(implicit ec: ExecutionContext): Future[CResult[Boolean]] = ???

  def save[A](key: String, a: A)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[CResult[A]] = {

    val json: String = fmt.write(a).toString

    tmap += ( key -> json )

    logger.debug(s"Wrote Document for Key: $key with $json")
    Future { a.s }
  }

  def save[A](key: String, a: A, exp: Int, rule: PersistenceRule)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[CResult[A]] = ???

  def byKey[A](key: String)(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[A] = {

    tmap.get(key).map { doc =>

      val json: JsValue = JsonParser(ParserInput(doc))

      logger.debug(s"Retrieved Document by Key: $key as ${json.compactPrint}")

      try { fmt.read(json).s }
      catch {
        case e: DeserializationException =>
          logger.error(s"Unable to Unmarshal JSON: ${json.compactPrint} with ${e.getMessage}")
          UnmarshalException(s"Unmarshal Failure: ${e.getMessage}").f
      }
    } getOrElse { StoreException(s"Document Not Found for Key: $key").f }
  }

  def update[A,B,C](key: String, b: B)(f1: (A,B) => C)(implicit fmt1: RootJsonFormat[A], fmt2: RootJsonFormat[C], ec: ExecutionContext): Future[CResult[C]] = {

    tmap.get(key).map { doc =>

      val a: A = fmt1.read( JsonParser(ParserInput(doc)) )

      val merged: C = f1(a,b)
      val json = fmt2.write(merged)

      tmap += ( key -> json.toString )

      logger.debug(s"Updated Document for Key: $key with $json")
      Future { merged.s }

    } getOrElse {

      logger.info(s"Attempted to Update Invalid Document for Key: $key with $b")
      Future { StoreException("Document Not Found").f }
    }
  }

  def delete(key: String)(implicit ec: ExecutionContext): Future[CResult[Boolean]] = {

    tmap.get(key).map { json =>

      tmap -= key

      logger.debug(s"Document Marked to Expire for Key: $key")

      Future { true.s }

    } getOrElse {

      logger.info(s"Attempt to Expire Invalid Document for Key: $key")
      Future { false.s }
    }
  }

  def query[A](designDocumentName: String, viewName: String, keyParts: List[Object])(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[List[A]] = ???
}

object Memory {

  def apply(): Memory = new Memory()
}
