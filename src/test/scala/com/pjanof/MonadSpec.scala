package com.pjanof

import org.scalatest._

class MonadSpec extends FlatSpec with Matchers {

import FreeExample._

  "The Identity Monad" should "adhere to the monad laws" in {

    // Option identity
    assert(identityMonad.pure(Some("foo")) === Some("foo"))
    assert(identityMonad.flatMap(Some("foo")) === "foo")

    assert(identityMonad.pure(None) === None)
    assert(identityMonad.flatMap(None) === None)
  }
}
