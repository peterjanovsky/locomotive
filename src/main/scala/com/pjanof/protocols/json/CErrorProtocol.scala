package com.pjanof.protocols.json

import com.pjanof.results.CResults._

import spray.json._

trait CErrorProtocol extends DefaultJsonProtocol {

  implicit object CErrorFormat extends RootJsonFormat[CError] {

    def write(error: CError): JsValue = JsString(error.msg)

    def read(value: JsValue): CError = value match {
      case JsString(msg) => GenericException(msg)
      case e => deserializationError(s"Expected CError as JsString, but got $e")
    }
  }
}
