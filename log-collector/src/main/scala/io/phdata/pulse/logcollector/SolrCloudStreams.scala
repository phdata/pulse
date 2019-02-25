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

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.ActorRef
import akka.stream.scaladsl.{ RunnableGraph, Sink, Source }
import akka.stream.{ ActorAttributes, OverflowStrategy }
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.common.{ DocumentConversion, SolrService }

import scala.concurrent.Future
import scala.concurrent.duration._

class SolrCloudStreams(solrService: SolrService) extends LazyLogging {
  val DEFAULT_MAX_BUFFER_SIZE      = 512000
  val DEFALUT_GROUP_SIZE           = 1000
  val DEFAULT_BATCH_FLUSH_DURATION = 1
  val DEFAULT_OVERFLOW_STRATAGY    = OverflowStrategy.fail

  val MAX_SUBSTREAMS = 1000 // If the number of apps being processed exceeds this some will be dropped

  val maxBuffersize: Int =
    sys.props
      .get("pulse.collector.stream.buffer.max")
      .map(_.toInt)
      .getOrElse(DEFAULT_MAX_BUFFER_SIZE)
  val batchFlushDuration = {
    val property =
      sys.props
        .get("pulse.collector.stream.flush.seconds")
        .map(_.toInt)
        .getOrElse(DEFAULT_BATCH_FLUSH_DURATION)

    Duration(property, TimeUnit.SECONDS)

  }
  val solrBatchSize =
    sys.props.get("pulse.collector.stream.batch.size").map(_.toInt).getOrElse(DEFALUT_GROUP_SIZE)

  val overflowStrategy = {
    sys.props
      .get("pulse.collector.stream.overflow.strategy")
      .map { strategy =>
        strategy.trim.toLowerCase match {
          case "fail"         => OverflowStrategy.fail
          case "drophead"     => OverflowStrategy.dropHead
          case "droptail"     => OverflowStrategy.dropTail
          case "dropnew"      => OverflowStrategy.dropNew
          case "dropbuffer"   => OverflowStrategy.dropBuffer
          case "backpressure" => OverflowStrategy.backpressure
        }
      }
      .getOrElse(DEFAULT_OVERFLOW_STRATAGY)
  }

  /**
   * Solr Cloud [[Sink]] for [[LogEvent]]s
   */
  private def solrSink: Sink[(String, Seq[LogEvent]), Future[Done]] =
    Sink
      .foreach[(String, Seq[LogEvent])] {
        case (appName, events) =>
          val latestCollectionAlias = s"${appName}_latest"
          logger.trace(s"Saving $latestCollectionAlias LogEvent: ${events.toString}")
          try {
            solrService.insertDocuments(latestCollectionAlias,
                                        events.map(DocumentConversion.toSolrDocument))
          } catch {
            case e: Exception => logger.error("Error posting documents to solr", e)
          }
      }
      // Give the solrSink its own dispatcher so blocking in the dispatcher won't affect
      // the stream or the http server.
      .withAttributes(ActorAttributes.dispatcher("akka.actor.solr-dispatcher"))

  /**
   * This Graph will
   * - Group messages by application
   * - Once the size reaches a threshold or max time is reached, flush to the Solr Sink
   */
  val groupedInsert: RunnableGraph[ActorRef] = {
    Source
      .actorRef[(String, LogEvent)](maxBuffersize, overflowStrategy)
      .groupBy(MAX_SUBSTREAMS, x => x._1) // group by the application name
      .groupedWithin(solrBatchSize, batchFlushDuration)
      .map(x => (x(0)._1, x.map(_._2))) // get application name from the first tuple (they're all the same), and a sequence of LogEvents
      .mergeSubstreams
      .to(solrSink)
  }

}
