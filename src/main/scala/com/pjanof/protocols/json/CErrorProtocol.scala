package com.pjanof.protocols.json

import com.pjanof.results.CResults._

import spray.json._

trait CErrorProtocol extends DefaultJsonProtocol {

  implicit object CErrorFormat extends RootJsonFormat[CError] {

    def write(error: CError): JsValue = JsObject(
      "msg" -> JsString(error.msg)
      , "type" -> JsString(error.getClass.getName))

    def read(value: JsValue): CError = value.asJsObject.getFields("msg", "type") match {

      case Seq( JsString(msg), JsString("StoreException") ) => StoreException(msg)
      case Seq( JsString(msg), JsString("MarshalException") ) => MarshalException(msg)
      case Seq( JsString(msg), JsString("UnmarshalException") ) => UnmarshalException(msg)
      case Seq( JsString(msg), JsString("ServiceException") ) => ServiceException(msg)
      case Seq( JsString(msg), JsString("ClientException") ) => ClientException(msg)
      case Seq( JsString(msg), JsString("GenericException") ) => GenericException(msg)
      case Seq( JsString(msg), JsString("InternalException") ) => InternalException(msg)

      case e => deserializationError(s"Expected CError as JsObject, but got $e")
    }
  }
}
