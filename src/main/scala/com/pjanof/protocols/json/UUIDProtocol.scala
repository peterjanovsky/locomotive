package com.pjanof.protocols.json

import spray.json._

import java.util.UUID

trait UUIDProtocol extends DefaultJsonProtocol {

  implicit object UUIDFormat extends RootJsonFormat[UUID] {

    def write(uuid: UUID): JsValue = JsString(uuid.toString)

    def read(value: JsValue): UUID = value match {
      case JsString(uuid) => UUID.fromString(uuid)
      case e => deserializationError(s"Expected UUID as JsString, but got $e")
    }
  }
}
