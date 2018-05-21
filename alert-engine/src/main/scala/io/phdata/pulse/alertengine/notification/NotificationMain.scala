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
import io.phdata.pulse.alertengine.{ AlertRule, MailAlertProfile, TriggeredAlert }

object NotificationMain extends LazyLogging {
  def main(args: Array[String]): Unit = {
    println(
      s"args: <smtp server> <smtp server port> <smtp-tls> <username> <password> <email addresses>")

    val password = if (args(4) == "") None else Some(args(4))
    val service =
      new MailNotificationService(args(0), args(1).toLong, args(3), password, args(2).toBoolean)
    val triggeredAlert =
      TriggeredAlert(AlertRule("query", 1, None, List()), "", Seq())
    val mailProfile = MailAlertProfile("b", List(args(5)))
    service.notify(Seq(triggeredAlert), mailProfile)
  }
}
