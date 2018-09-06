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

import io.phdata.pulse.alertengine._
import org.scalatest.FunSuite

import scala.io.Source._

class MailNotificationServiceTest extends FunSuite {

  val doc = TestSolrDocuments("fatal")

  val alertrule = TestAlertrules("slackWithRetryInterval10")
  val alertrule2 = TestAlertrules("slackWithRetryInterval20")

  val profile = TestMailAlertProfiles("withOneAddress")
  val profile2 = TestMailAlertProfiles("withTwoAddresses")

  val triggeredalert = TestTriggers("slackWithSparkAppAndTotalNumFound23")
  val triggeredalert2 = TestTriggers("slackWithSparkAppAndTotalNumFound15")

  test("sending one email to an address") {
    if (new java.io.File("alert-engine/scripts/mail-password.txt").exists) {
      val pwd = fromFile("alert-engine/scripts/mail-password.txt").getLines.mkString
      val mail =
        new MailNotificationService("smtp.gmail.com", 25, "testing@phdata.io", Some(pwd), true)
      mail.notify(Seq(triggeredalert), profile)
    } else {
      println("no password, skip the test")
    }
  }

  test("sending one email to an profile with two addresses testing1 and testing") {
    if (new java.io.File("alert-engine/scripts/mail-password.txt").exists) {
      val pwd = fromFile("alert-engine/scripts/mail-password.txt").getLines.mkString
      val mail =
        new MailNotificationService("smtp.gmail.com", 587, "testing@phdata.io", Some(pwd), true)
      mail.notify(Seq(triggeredalert), profile2)
    } else {
      println("no password, skip the test")
    }
  }

  test("sending two triggered alerts to profile") {
    if (new java.io.File("alert-engine/scripts/mail-password.txt").exists) {
      val pwd = fromFile("alert-engine/scripts/mail-password.txt").getLines.mkString
      val mail =
        new MailNotificationService("smtp.gmail.com", 587, "testing@phdata.io", Some(pwd), true)
      mail.notify(Seq(triggeredalert, triggeredalert2), profile)
    } else {
      println("no password, skip the test")
    }
  }

  test("sending many alerts to a profile with many addresses") {
    if (new java.io.File("alert-engine/scripts/mail-password.txt").exists) {
      val pwd = fromFile("alert-engine/scripts/mail-password.txt").getLines.mkString
      val mail =
        new MailNotificationService("smtp.gmail.com", 587, "testing@phdata.io", Some(pwd), true)
      mail.notify(Seq(triggeredalert, triggeredalert2), profile2)
    } else {
      println("no password, skip the test")
    }
  }
}
