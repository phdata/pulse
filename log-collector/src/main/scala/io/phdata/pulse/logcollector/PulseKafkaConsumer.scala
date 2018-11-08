/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import java.util.{Collections, Properties}

import akka.actor.{ActorRef, ActorSystem}
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import io.phdata.pulse.common.domain.LogEvent
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.{JsonSupport, SolrService}
import org.apache.solr.common.SolrException
import spray.json.JsonParser.ParsingException

import scala.collection.JavaConverters._
import spray.json._

/**
  * Consumes JSON strings from a given broker and topic
  * parses into LogEvent
  * sends to Solr Cloud
 * @param solrService
 */
class PulseKafkaConsumer(solrService: SolrService) extends JsonSupport with LazyLogging {
  val MAX_TIMEOUT = 100

  implicit val solrActorSystem: ActorSystem             = ActorSystem()
  implicit val solrActorMaterializer: ActorMaterializer = ActorMaterializer.create(solrActorSystem)

  val solrInputStream: ActorRef = new SolrCloudStreams(solrService).groupedInsert.run()

  def read(consumerProperties: Properties, topic: String): Unit = {
    val consumer = new KafkaConsumer[String, String](consumerProperties)

    consumer.subscribe(Collections.singletonList(topic))

    while (true) {
      try {
        val records = consumer.poll(MAX_TIMEOUT)
        for (record <- records.asScala) {
          logger.trace("KAFKA: Consuming " + record.value() + " from topic: " + topic)
          val logEvent = record
            .value()
            .parseJson
            .convertTo[LogEvent]
          solrInputStream ! (logEvent.application.get, logEvent)
        }
      } catch {
        case p: ParsingException => logger.error(p.getMessage)
        case e: Exception => logger.error("Error consuming messages from kafka broker", e)
      }
    }
  }
}
