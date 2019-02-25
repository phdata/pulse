/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pulse.logcollector

import java.util.{ Collections, Properties }

import akka.actor.{ ActorRef, ActorSystem }
import org.apache.kafka.clients.consumer.KafkaConsumer
import io.phdata.pulse.common.domain.LogEvent
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.{ JsonSupport, SolrService }
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
        case p: ParsingException => logger.error("Error parsing message from kafka broker", p)
        case e: Exception        => logger.error("Error consuming messages from kafka broker", e)
      }
    }
  }
}
