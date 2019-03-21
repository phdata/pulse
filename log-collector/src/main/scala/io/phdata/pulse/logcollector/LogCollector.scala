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

import java.util.Properties
import java.util.concurrent.Executors

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.solr.client.solrj.impl.CloudSolrServer

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

/**
 * LogCollector Application exposes a Http Endpoint for capturing LogEvents.
 */
object LogCollector extends LazyLogging {

  /**
   * Main starting point
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {

    val cliParser = new LogCollectorCliParser(args)

    val solrServer  = new CloudSolrServer(cliParser.zkHosts())
    val solrService = new SolrService(cliParser.zkHosts(), solrServer)

    // Akka System
    implicit val actorSystem: ActorSystem   = ActorSystem()
    implicit val materializer: Materializer = ActorMaterializer.create(actorSystem)
    implicit val ec                         = actorSystem.dispatchers.lookup("akka.actor.http-dispatcher")

    val routes = new LogCollectorRoutes(solrService)

    def serve(port: Int): Unit = {
      // Starts Http Service
      val httpServerFuture = Http().bindAndHandle(routes.routes, "0.0.0.0", port = port)(
        materializer) map { binding =>
        logger.info(s"Log Collector interface bound to: ${binding.localAddress}")
      }

      httpServerFuture.onComplete {
        case Success(v) => ()
        case Failure(e) => throw new RuntimeException(e)
      }

      // Start LogCollector HttpService
      Await.ready(
        httpServerFuture,
        Duration.Inf
      )
    }

    cliParser.mode() match {
      case "kafka" => {
        consume(solrService, cliParser.zkHosts(), cliParser.port(), cliParser.topic())
      }
      case _ => {
        serve(cliParser.port())
      }
    }
  }

  // Starts Kafka Consumer
  def consume(solrService: SolrService, zkHost: String, port: Int, topic: String): Unit = {
    val kafkaConsumer      = new PulseKafkaConsumer(solrService)
    val kafkaConsumerProps = new Properties()

    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$zkHost:$port")
    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "pulse-kafka")

    kafkaConsumer.read(kafkaConsumerProps, topic)
  }
}
