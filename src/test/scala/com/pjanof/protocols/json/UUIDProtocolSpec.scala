package com.pjanof.protocols.json

import java.util.UUID
import org.scalatest._

import spray.json._

trait UUIDProtocols extends UUIDProtocol

class UUIDProtocolSpec extends FlatSpec with Matchers with UUIDProtocols {

  "UUID" should "marshal to/from JsString" in {

    val uuid: UUID = UUID.randomUUID

    val value: JsValue = uuid.toJson
    value should be ( JsString(uuid.toString) )

    value.convertTo[UUID] should be ( uuid )
  }

  it should "fail with DeserializationException when attempting to unmarshal a value other than JsString" in {
    intercept[DeserializationException] {
      val value: JsNumber = JsNumber(0)
      value.convertTo[UUID]
    }
  }

  it should "marshal to/from JsString when combined with other protocols" in {

    case class Test(a: String, b: Int, uuid: UUID)

    implicit val testFormat = jsonFormat3(Test.apply)

    val uuid = UUID.randomUUID
    val test = Test("foo", 1, uuid)

    val value: JsValue = test.toJson
    value should be ( s"""{"a":"foo","b":1,"uuid":"${uuid.toString}"}""".parseJson )

    value.convertTo[Test] should be ( test )
  }
}
