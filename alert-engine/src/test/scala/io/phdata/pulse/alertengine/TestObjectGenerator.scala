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

package io.phdata.pulse.alertengine

import org.apache.solr.common.SolrDocument

/**
  * TestObjectGenerator helps avoid redundant test object creation by providing methods
  * for generating default and custom objects for testing.
  *
  */
object TestObjectGenerator {

  /**
    * solrDocumentTestObject is used for generating defualt and custom solr document test objects.
    *
    * @param id
    * @param category
    * @param timestamp
    * @param level
    * @param message
    * @param threadName
    * @param throwable
    * @return solrDocument
    */
  def solrDocumentTestObject(
                              id: String = "123",
                              category: String = "test",
                              timestamp: String = "2018-04-06 10:15:00",
                              level: String = "FATAL",
                              message: String = "The service is down.",
                              threadName: String = "thread3",
                              throwable: String = "NullPointerException"

                            ): SolrDocument = {

    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", id)
    doc.addField("category", category)
    doc.addField("timestamp", timestamp)
    doc.addField("level", level)
    doc.addField("message", message)
    doc.addField("threadName", threadName)
    doc.addField("throwable", throwable)

    doc
  }
}
