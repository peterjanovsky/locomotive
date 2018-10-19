package com.pjanof.stores

import com.pjanof.results.CResults._

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import com.typesafe.scalalogging.StrictLogging

import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

import java.lang.Exception
import java.util.Properties

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

/*
class Kafka[K,V] private(config: KafkaConfig) extends MessageStore
  with StrictLogging {

  private class SendCallback extends Callback {

    val promise = Promise[CResult[Boolean]]

    def onCompletion(metadata: RecordMetadata, e: Exception): Unit =

      if ( e == null ) {

        logger.debug(s"Message Written at Offset: ${metadata.offset} with Topic: ${
          metadata.topic} and Partition: ${metadata.offset}")

        promise.success(true.s)

      } else {

        logger.error(s"Unable to Write Message with ${e.getMessage}")
        promise.success(StoreException("Write Failure: ${e.getMessage}").f)

      }
  }

  private val producer: KafkaProducer[K,V] =
    new KafkaProducer[K,V](config.toProperties)

  def close()(implicit ec: ExecutionContext): Future[CResult[Boolean]] = ???

  def send(topic: String, k: K, v: V)(implicit ec: ExecutionContext): Future[CResult[Boolean]] = {

    val record: ProducerRecord[K,V] = new ProducerRecord(topic, k, v)

    val callback = new SendCallback

    producer.send(record, callback)
    callback.promise.future
  }
}

object Kafka {

  def apply[K,V](config: KafkaConfig): Kafka[K,V] = new Kafka[K,V](config)
}

trait StoreConfig

class KafkaConfig private(config: Config) extends StoreConfig {

  def toProperties(): Properties =
    config.entrySet.asScala.foldLeft( new Properties ){ (acc,elem) => {
      acc.put(elem.getKey, elem.getValue.unwrapped)
      acc } }
}

object KafkaConfig {

  def apply(config: Config): KafkaConfig = new KafkaConfig(config)
}
*/
