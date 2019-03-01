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

package io.phdata.pulse.common

import io.phdata.pulse.common.domain.LogEvent
import org.apache.solr.common.{ SolrDocument, SolrInputDocument }

/**
 *
 */
object DocumentConversion {

  /**
   * Convert a [[Map[String, String]]] to a [[SolrInputDocument]]
   * @param event [[Map[String, String]]] to convert
   * @return The document as a [[SolrDocument]]
   */
  def mapToSolrDocument(event: Map[String, String]): SolrInputDocument = {
    val doc = new SolrInputDocument()
    event.foreach(field => doc.addField(field._1, field._2))
    doc
  }

  /**
   * Convert a [[SolrDocument]] to a [[LogEvent]]
   * @param document SolrDocument to convert
   * @return The document as a [[LogEvent]]
   */
  def toLogEvent(document: SolrDocument): LogEvent =
    LogEvent(
      Option(document.get("id").toString),
      document.get("category").toString,
      document.get("timestamp").toString,
      document.get("level").toString,
      document.get("message").toString,
      document.get("threadName").toString,
      Some(document.get("throwable").toString)
    )
}
