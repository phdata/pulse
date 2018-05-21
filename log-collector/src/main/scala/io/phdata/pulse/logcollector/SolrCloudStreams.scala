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

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.common.{ DocumentConversion, SolrService }

import scala.concurrent.Future
import scala.concurrent.duration._

class SolrCloudStreams(solrService: SolrService) extends LazyLogging {

  val GROUP_SIZE     = 10000 // Number of elements to put into a group
  val GROUP_MAX_TIME = 3 seconds // If a grouping exceeds this time it will stop short of GROUP_SIZE
  val MAX_SUBSTREAMS = 1000 // If the number of apps being processed exceeds this some will be dropped

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec                       = actorSystem.dispatchers.lookup("akka.actor.solr-dispatcher")

  val groupedInsert =
    Source
      .actorRef[(String, LogEvent)](Int.MaxValue, OverflowStrategy.fail)
      .async
      .groupBy(MAX_SUBSTREAMS, x => x._1) // group by the application name
      .async
      .groupedWithin(GROUP_SIZE, GROUP_MAX_TIME) // group 1000 records or within one second, whichever comes first
      .async
      .map(x => (x(0)._1, x.map(_._2))) // get application name from the first tuple (they're all the same), and a sequence of LogEvents
      .async
      .mergeSubstreams
      .async
      .mapAsyncUnordered(10)(x => save(x._1, x._2))
      .to(Sink.ignore)
      .async

  private def save(appName: String, events: Seq[LogEvent]): Future[Unit] =
    Future {
      val latestCollectionAlias = s"${appName}_latest"
      logger.trace(s"Saving $latestCollectionAlias LogEvent: ${events.toString}")
      try {
        solrService.insertDocuments(latestCollectionAlias,
                                    events.map(DocumentConversion.toSolrDocument))
      } catch {
        case e: Exception => logger.error("Error posting documents to solr", e)
      }
    }

}
