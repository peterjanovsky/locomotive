package com.pjanof.io.actors

import akka.actor.Actor

import com.typesafe.scalalogging.StrictLogging

abstract class InstrumentedActor extends Actor with StrictLogging {

  override def postStop() {
    logger.warn(s"Stopping Actor: ${getClass.getName}")
    super.postStop
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    logger.error(s"Restarting Actor: $reason")
    super.preRestart(reason, message)
  }

  override def preStart() {
    logger.debug(s"Starting Actor: ${getClass.getName}")
    super.preStart
  }
}
