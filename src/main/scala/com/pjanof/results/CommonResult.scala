package com.pjanof.results

import scalaz.-\/
import scalaz.\/-
import scalaz.\/
import scalaz.EitherT
import scalaz.Scalaz._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object CResults {

  sealed trait CError {
    val msg: String
  }

  case class StoreException(msg: String) extends CError
  case class MarshalException(msg: String) extends CError
  case class UnmarshalException(msg: String) extends CError
  case class ServiceException(msg: String) extends CError
  case class ClientException(msg: String) extends CError
  case class GenericException(msg: String) extends CError
  case class InternalException(msg: String) extends CError

  type CResult[+A] = CError \/ A

  implicit class CResultOps[A](a: A) {
    def s: CResult[A] = \/.right(a)
  }

  implicit class CErrorOps[E <: CError](e: E) {
    def f[A]: CResult[A] = \/.left(e)
  }

  type CFResult[A] = EitherT[Future, CError, A]

  implicit class CFResultOps[A](a: A)(implicit ec: ExecutionContext) {
    def sf: CFResult[A] = EitherT(Future(a.s))
  }

  implicit class CFErrorOps[E <: CError](e: E)(implicit ec: ExecutionContext) {
    def ff[A]: CFResult[A] = EitherT(Future(e.f))
  }

  def fromFuture[A](fa: Future[A])(implicit ec: ExecutionContext): CFResult[A] = EitherT(fa.map(\/.right(_)))

  def fromEither[A, B](eb: B => CError)(ab: B \/ A): CFResult[A] = EitherT(Future.successful(ab.leftMap(eb)))

  def fromOption[A](e: => CError)(oa: Option[A]): CFResult[A] = EitherT(Future.successful(oa \/> e))
}
