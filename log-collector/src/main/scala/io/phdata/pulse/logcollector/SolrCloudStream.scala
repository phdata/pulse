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

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService

/*
  Writes messages to Solr using some stream transformation logic.
 */
class SolrCloudStream(solrService: SolrService, streamParams: StreamParams = StreamParams())
    extends Stream[Map[String, String]](streamParams)
    with LazyLogging {

  /**
   * Save a batch of events to Solr
   *
   * @param appName Name of the application
   * @param events  Batch of events
   * @return An asynchronous task to be executed
   */
  override private[logcollector] def save(appName: String, events: Seq[Map[String, String]]): Unit =
    if (events.nonEmpty) {
      val latestCollectionAlias = s"${appName}_latest"
      logger.debug(s"Saving batch of ${events.length} to collection '$latestCollectionAlias}'")
      solrService.insertDocuments(latestCollectionAlias, events)
      logger.debug(s"Save to $latestCollectionAlias successful")
    }
}
