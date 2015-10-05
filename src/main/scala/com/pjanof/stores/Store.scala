package com.pjanof.stores

import com.pjanof.results.CResults._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import spray.json.RootJsonFormat

import org.slf4j.LoggerFactory

trait Store {

  def close()(implicit ec: ExecutionContext): Future[CResult[Boolean]]
}

trait DocumentStore extends Store {

  sealed trait PersistenceRule
  case object Master extends PersistenceRule
  case object AtLeastOneNode extends PersistenceRule

  type PersistenceRuleTransformer[A] = PersistenceRule => A

  def save[A](key: String, a: A)(implicit fmt: RootJsonFormat[A]
    , ec: ExecutionContext): Future[CResult[A]]

  def save[A](key: String, a: A, exp: Int, rule: PersistenceRule)
    (implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[CResult[A]]

  def byKey[A](key: String)
    (implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[A]

  def update[A,B,C](key: String, b: B)(f1: (A,B) => C)(implicit fmt1: RootJsonFormat[A]
    , fmt2: RootJsonFormat[C], ec: ExecutionContext): Future[CResult[C]]

  def delete(key: String)(implicit ec: ExecutionContext): Future[CResult[Boolean]]

  def query[A](designDocumentName: String, viewName: String, keyParts: List[Object])
    (implicit fmt: RootJsonFormat[A], ec: ExecutionContext): CResult[List[A]]
}

trait MessageStore extends Store {

  def send[K,V](topic: String, k: K, v: V)(implicit ec: ExecutionContext): Future[CResult[Boolean]]
}
