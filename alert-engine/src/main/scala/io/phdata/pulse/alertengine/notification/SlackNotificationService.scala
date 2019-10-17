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

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.alertengine.{ SlackAlertProfile, TriggeredAlert }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

class SlackNotificationService() extends LazyLogging {
  def notify(alerts: Iterable[TriggeredAlert], profile: SlackAlertProfile): Unit =
    for (alert <- alerts) {
      val formattedBody    = NotificationFormatter.formatMessage(alert)
      val formattedSubject = NotificationFormatter.formatSubject(alert)
      sendSlackMsg(profile.url, formattedSubject + formattedBody)
    }

  def sendSlackMsg(url: String, message: String): Unit = {
    val httpClient = new DefaultHttpClient()
    try {
      val httpPost = new HttpPost(url)
      val jsonStr =
        s"""
           |{"text": "$message
           |---------------------------------------------"}
           |""".stripMargin
      val entity = new StringEntity(jsonStr)
      httpPost.setEntity(entity)
      httpPost.setHeader("Content-type", "application/json")
      httpClient.execute(httpPost)
    } catch {
      case e: Exception =>
        logger.error(s"Error sending slack message $message", e)
    } finally {
      httpClient.getConnectionManager.shutdown()
    }
  }
}
