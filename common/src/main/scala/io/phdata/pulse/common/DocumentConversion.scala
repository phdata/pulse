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

  def toSolrDocument(event: LogEvent): SolrInputDocument = {
    val doc = new SolrInputDocument()
    event.id.foreach(id => doc.addField("id", id))
    doc.addField("category", event.category)
    doc.addField("timestamp", event.timestamp)
    doc.addField("level", event.level)
    doc.addField("message", event.message)
    doc.addField("threadName", event.threadName)
    doc.addField("throwable", event.throwable.getOrElse(""))

    // If the event properties exist, add each property to the document. The fields will be added as
    // dynamic [[String]] fields
    event.properties.fold() { properties =>
      properties.foreach {
        case (k: String, v: String) =>
          doc.addField(s"$k", v)
      }
    }
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