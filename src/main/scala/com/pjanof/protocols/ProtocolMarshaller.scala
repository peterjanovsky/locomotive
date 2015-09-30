package com.pjanof.protocols

import com.pjanof.protocols.json.CErrorProtocol
import com.pjanof.results.CResults._

import scalaz.-\/
import scalaz.\/-

import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait ProtocolMarshaller {

  def marshal[A](result: CResult[A])(implicit fmt: RootJsonFormat[A]): JsValue

  def marshal[A](result: CFResult[A])(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[JsValue]
}

object JSONMarshaller extends ProtocolMarshaller with CErrorProtocol {

  private def handle[A](result: CResult[A])(implicit fmt: RootJsonFormat[A]): JsValue =
    result match {
      case -\/(e) => e.toJson
      case \/-(a) => fmt.write(a)
    }

  def marshal[A](result: CResult[A])(implicit fmt: RootJsonFormat[A]): JsValue = handle(result)

  def marshal[A](result: CFResult[A])(implicit fmt: RootJsonFormat[A], ec: ExecutionContext): Future[JsValue] =
    result.run.map { handle(_) }
}
