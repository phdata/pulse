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

  val doc2: SolrDocument = new SolrDocument()
  doc2.addField("id", "1970-01-01T00:00:00Z")
  doc2.addField("category", "formatting")
  doc2.addField("timestamp", "2018-04-06 15:15:00Z")
  doc2.addField("level", "WARN")
  doc2.addField("message", "Make the service better.")
  doc2.addField("threadName", "thread5")
  doc2.addField("throwable", "OutOfMemoryException")

  val alertrule = AlertRule("query0000000000", 10, Some(0), List("a", "slack"))

  val triggeredalert = TriggeredAlert(alertrule, "Spark", Seq(doc))
  val triggeredalert2 =
    TriggeredAlert(alertrule, "PipeWrench", Seq(doc, doc2))

  test("subject content testing") {
    assertResult("New alert in application Spark")(
      NotificationFormatter.formatSubject(triggeredalert))
    assertResult("New alert in application PipeWrench")(
      NotificationFormatter.formatSubject(triggeredalert2))
  }

  test("body with 1 log testing") {
    println(NotificationFormatter.formatMessage(triggeredalert))
  }

  test("body with 2 logs testing") {
    println(NotificationFormatter.formatMessage(triggeredalert2))
  }
}
