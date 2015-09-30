package com.pjanof.protocols.json

import org.joda.time.DateTime
import org.scalatest._

import spray.json._

trait DateTimeProtocols extends DateTimeProtocol {
  implicit val dateTimeFormat = DateTimeFormat
}

class DateTimeProtocolSpec extends FlatSpec with Matchers with DateTimeProtocols {

  "DateTime" should "marshal to/from JsString" in {

    val dt: DateTime = DateTime.now

    val value: JsValue = dt.toJson
    value should be ( JsString(formatter.print(dt)) )

    value.convertTo[DateTime] should be ( dt )
  }

  it should "fail with DeserializationException when attempting to unmarshal a value other than JsString" in {
    intercept[DeserializationException] {
      val value: JsNumber = JsNumber(0)
      value.convertTo[DateTime]
    }
  }

  it should "marshal to/from JsString when combined with other protocols" in {

    case class Test(a: String, b: Int, dt: DateTime)

    implicit val testFormat = jsonFormat3(Test.apply)

    val dt = DateTime.now
    val test = Test("foo", 1, dt)

    val value: JsValue = test.toJson
    value should be ( s"""{"a":"foo","b":1,"dt":"${formatter.print(dt)}"}""".parseJson )

    value.convertTo[Test] should be ( test )
  }
}
