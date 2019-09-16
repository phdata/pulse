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

import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService
import io.phdata.pulse.solr.SolrProvider
import org.apache.kudu.client.KuduClient.KuduClientBuilder

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
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
  def main(args: Array[String]): Unit =
    System.getProperty("java.security.auth.login.config") match {
      case null => {
        logger.info(
          "java.security.auth.login.config is not set, continuing without kerberos authentication")
      }
      case _ => {
        KerberosContext.scheduleKerberosLogin(0, 9, TimeUnit.HOURS)
      }

      start(args)

    }

  private def start(args: Array[String]): Unit = {
    val cliParser = new LogCollectorCliParser(args)

    val solrService = SolrProvider.create(cliParser.zkHosts().split(",").toList)
    val solrStream  = new SolrCloudStream(solrService)

    val kuduClient =
      cliParser.kuduMasters.toOption.map(masters => new KuduClientBuilder(masters).build())

    val kuduService =
      kuduClient.map(client => KerberosContext.runPrivileged(new KuduService(client)))

    val routes = new LogCollectorRoutes(solrStream, kuduService)

    cliParser.mode() match {
      case "kafka" => {
        kafka(solrService, cliParser.kafkaProps(), cliParser.topic())
      }
      case _ => {
        http(cliParser.port(), routes)
      }
    }
  }

  // Starts Http Service
  def http(port: Int, routes: LogCollectorRoutes): Future[Unit] = {
    implicit val actorSystem: ActorSystem   = ActorSystem()
    implicit val ec                         = actorSystem.dispatchers.lookup("akka.actor.http-dispatcher")
    implicit val materializer: Materializer = ActorMaterializer.create(actorSystem)

    val httpServerFuture = Http().bindAndHandle(routes.routes, "0.0.0.0", port)(materializer) map {
      binding =>
        logger.info(s"Log Collector interface bound to: ${binding.localAddress}")
    }

    httpServerFuture.onComplete {
      case Success(v) => ()
      case Failure(e) => throw new RuntimeException(e)
    }

    Await.ready(
      httpServerFuture,
      Duration.Inf
    )
  }

  // Starts Kafka Consumer
  def kafka(solrService: SolrService, kafkaProps: String, topic: String): Unit = {

    val solrCloudStream = new SolrCloudStream(solrService)

    val kafkaConsumer      = new PulseKafkaConsumer(solrCloudStream)
    val kafkaConsumerProps = new Properties()

    kafkaConsumerProps.load(new FileInputStream(kafkaProps))

    kafkaConsumer.read(kafkaConsumerProps, topic)
  }
}
