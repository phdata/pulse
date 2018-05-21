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
import io.phdata.pulse.alertengine.{ MailAlertProfile, TriggeredAlert }

class MailNotificationService(smtpServer: String,
                              port: Long = 25,
                              username: String,
                              password: Option[String],
                              use_smtp_tls: Boolean)
    extends LazyLogging {
  def notify(alerts: Iterable[TriggeredAlert], profile: MailAlertProfile): Unit = {
    logger.info(s"starting notification for profile ${profile.name}")
    val mailer = new Mailer(smtpServer, port, username, password, use_smtp_tls)
    for (alert <- alerts) {
      val formattedBody    = NotificationFormatter.formatMessage(alert)
      val formattedSubject = NotificationFormatter.formatSubject(alert)
      logger.info(s"sending alert")
      mailer.sendMail(profile.addresses, formattedSubject, formattedBody)
    }
  }
}
