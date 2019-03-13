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

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.common.{ JsonSupport, SolrService }

/**
 * Http Rest Endpoint
 */
class LogCollectorRoutes(solrService: SolrService) extends JsonSupport with LazyLogging {

  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  val stream = new SolrCloudStream(solrService)

  /**
   * Defines /log routes
   */
  val routes =
  path("log") {
    get {
      complete("Log Collector")
    } ~
    post {
      // route example "/log?application=applicationName"
      parameter('application) { applicationName =>
        // create a streaming Source from the incoming json
        entity(as[LogEvent]) { logEvent =>
          logger.trace("received message")
          stream.put(applicationName, Util.logEventToFlattenedMap(logEvent))

          complete(HttpEntity(ContentTypes.`application/json`, "ok"))
        }
      }
    }
  } ~ path("v2" / "event" / Segment) { applicationName =>
    /**
     * Consumes a single log event
     */
    post {
      // route example "/log?application=applicationName"
      // create a streaming Source from the incoming json
      entity(as[LogEvent]) { logEvent =>
        logger.trace("received message")
        stream.put(applicationName, Util.logEventToFlattenedMap(logEvent))
        complete(HttpEntity(ContentTypes.`application/json`, "ok"))
      }
    }
  } ~ path("v2" / "events" / Segment) { applicationName =>
    /**
     * Consumes an array of log events
     */
    post {
      // create a streaming Source from the incoming json
      entity(as[Array[LogEvent]]) { logEvents =>
        logger.trace("received message")
        logEvents.foreach(logEvent =>
          stream.put(applicationName, Util.logEventToFlattenedMap(logEvent)))

        complete(HttpEntity(ContentTypes.`application/json`, "ok"))
      }
    }
  } ~ path("v1" / "json" / Segment) { applicationName =>
    /**
     * Consumes an array of json events
     */
    post {
      // create a streaming Source from the incoming json string
      entity(as[Array[Map[String, String]]]) { objects =>
        logger.trace("received message")

        objects.foreach(jsonMap => {
          stream.put(applicationName, jsonMap)
        })

        complete(HttpEntity(ContentTypes.`application/json`, "ok"))
      }
    }
  }

}
