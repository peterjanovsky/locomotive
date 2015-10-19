package com.pjanof.protocols.json

import com.pjanof.results.CResults._

import org.scalatest._

import spray.json._

trait CErrorProtocols extends CErrorProtocol

class CErrorProtocolSpecs extends FlatSpec with Matchers with CErrorProtocols {

  "CError" should "marshall to/from JsString" in {

    val msg = "store-exception"
    val exception: CError = StoreException(msg)

    val value: JsValue = exception.toJson
    value should be ( s"""{"msg":"$msg","type":"com.pjanof.results.CResults$$StoreException"}""".parseJson )

    value.convertTo[CError] should be ( exception )
  }
}
