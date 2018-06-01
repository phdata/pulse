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

import io.phdata.pulse.alertengine.TriggeredAlert

object NotificationFormatter {
  def formatSubject(alert: TriggeredAlert): String = {
    val sub = "New alert in application " + alert.applicationName
    sub
  }

  def formatMessage(alert: TriggeredAlert): String = {
    var docs = "Pulse Alert Triggered \n"
    val num  = alert.documents.length
    if (num > 1) {
      docs = docs.concat(s"""
                |  Application: ${alert.applicationName}
                |  Query: ${alert.rule.query}
                |  Displaying ${num} results of ${alert.rowcount} total found.
           """.stripMargin)
    } else {
      docs = docs.concat(s"""
                |  Application: ${alert.applicationName}
                |  Query: ${alert.rule.query}
                |  Displaying ${num} result of ${alert.rowcount} total found.
           """.stripMargin)
    }
    alert.documents.foreach { d =>
      val id         = d.get("id")
      val category   = d.get("category")
      val timestamp  = d.get("timestamp")
      val level      = d.get("level")
      val msg        = d.get("message")
      val threadName = d.get("threadName")
      val throwable  = d.get("throwable")
      docs = docs.concat(s"""
                            |      ID: $id
                            |      Category: $category
                            |      Timestamp: $timestamp
                            |      Level: $level
                            |      Message: $msg
                            |      Thread Name: $threadName
                            |      Throwable Exception: $throwable
           """.stripMargin)
    }
    docs
  }
}
