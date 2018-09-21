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

package io.phdata.pulse.alertengine.notification

import io.phdata.pulse.alertengine.TestObjectGenerator
import org.apache.solr.common.SolrDocument
import org.scalatest.FunSuite

class NotificationFormatterTest extends FunSuite {
  val doc: SolrDocument = TestObjectGenerator.solrDocument(level = "FATAL", message = "Service is down......")


  val alertRule = TestObjectGenerator.alertRule()

  val triggeredAlert = TestObjectGenerator.triggeredAlert()

  test("format subject content") {
    assertResult("Pulse alert triggered for 'Spark'")(
      NotificationFormatter.formatSubject(triggeredAlert))
  }

  test("format body (print only)") {
    println(NotificationFormatter.formatMessage(triggeredAlert))
  }

}
