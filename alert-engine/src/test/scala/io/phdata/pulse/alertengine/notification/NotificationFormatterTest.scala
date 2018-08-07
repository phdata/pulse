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

import io.phdata.pulse.alertengine.{ AlertRule, TriggeredAlert }
import org.apache.solr.common.SolrDocument
import org.scalatest.FunSuite

class NotificationFormatterTest extends FunSuite {
  val doc: SolrDocument = new SolrDocument()
  doc.addField("id", "1970-01-01T00:00:00Z")
  doc.addField("category", "test")
  doc.addField("timestamp", "2018-04-06 10:15:00Z")
  doc.addField("level", "FATAL")
  doc.addField("message", "The service is down.")
  doc.addField("threadName", "thread3")
  doc.addField("throwable", "NullPointerException")
  doc.addField("hostname", "edge1.valhalla.phdata.io")

  val alertrule = AlertRule("query0000000000", 10, Some(0), List("a", "slack"))

  val triggeredalert = TriggeredAlert(alertrule, "Spark", Seq(doc), 20)

  test("format subject content") {
    assertResult("Pulse alert triggered for 'Spark'")(
      NotificationFormatter.formatSubject(triggeredalert))
  }

  test("format body (print only)") {
    println(NotificationFormatter.formatMessage(triggeredalert))
  }

}
