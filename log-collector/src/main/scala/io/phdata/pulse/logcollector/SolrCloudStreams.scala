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

  val GROUP_SIZE     = 1000 // Number of elements to put into a group
  val GROUP_MAX_TIME = 1 seconds // If a grouping exceeds this time it will stop short of GROUP_SIZE
  val MAX_SUBSTREAMS = 1000 // If the number of apps being processed exceeds this some will be dropped

  /**
   * Solr Cloud [[Sink]] for [[LogEvent]]s
   */
//  private def solrSink: Sink[(String, Seq[LogEvent]), Future[Done]] =
  private def solrSink: Sink[(String, Seq[Map[String,String]]), Future[Done]] =
    Sink
//      .foreach[(String, Seq[LogEvent])] {
      .foreach[(String, Seq[Map[String,String]])] {
        case (appName, events) =>
          val latestCollectionAlias = s"${appName}_latest"
          logger.trace(s"Saving $latestCollectionAlias LogEvent: ${events.toString}")
          try {
            solrService.insertDocuments(latestCollectionAlias,
                                        events.map(DocumentConversion.mapToSolrDocument))
            logger.info("docs posted")   //TODO: REMOVE
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
//  val groupedInsert: RunnableGraph[ActorRef] = {
//    Source
//      .actorRef[(String, LogEvent)](Int.MaxValue, OverflowStrategy.dropNew)
//      .groupBy(MAX_SUBSTREAMS, x => x._1) // group by the application name
//      .groupedWithin(GROUP_SIZE, GROUP_MAX_TIME) // group 1000 records or within one second, whichever comes first
//      .map(x => (x(0)._1, x.map(_._2))) // get application name from the first tuple (they're all the same), and a sequence of LogEvents
//      .mergeSubstreams
//      .to(solrSink)
//  }
    val groupedInsert: RunnableGraph[ActorRef] = {
      Source
        .actorRef[(String, Map[String,String])](Int.MaxValue, OverflowStrategy.dropNew)
        .groupBy(MAX_SUBSTREAMS, x => x._1) // group by the application name
        .groupedWithin(GROUP_SIZE, GROUP_MAX_TIME) // group 1000 records or within one second, whichever comes first
        .map(x => (x(0)._1, x.map(_._2))) // get application name from the first tuple (they're all the same), and a sequence of LogEvents
        .mergeSubstreams
        .to(solrSink)
    }


}
