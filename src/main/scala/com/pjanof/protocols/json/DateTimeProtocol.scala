package com.pjanof.protocols.json

import com.typesafe.scalalogging.StrictLogging

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import spray.json._

trait DateTimeProtocol extends DefaultJsonProtocol with StrictLogging {

  protected val formatter = ISODateTimeFormat.basicDateTime

  private def handleError(dt: String, msg: String): DateTime = {

    logger.error(s"Unable to Parse DateTime: $dt with Error: [ $msg ]")
    deserializationError(s"Invalid DateTime: $dt")
  }

  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

    def write(dt: DateTime): JsValue = JsString(formatter.print(dt))

    def read(value: JsValue): DateTime = value match {

      case JsString(dt) =>

        try { formatter.parseDateTime(dt) } catch {

          case uoe: UnsupportedOperationException => handleError(dt, uoe.getMessage)
          case iae: IllegalArgumentException => handleError(dt, iae.getMessage)
        }

      case e => deserializationError(s"Expected DateTime as JsObject, but got $e")
    }
  }
}
