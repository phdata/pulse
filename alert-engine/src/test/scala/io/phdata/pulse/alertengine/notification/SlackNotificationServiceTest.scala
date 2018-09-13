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

import java.io.File

import io.phdata.pulse.alertengine._
import org.scalatest.FunSuite

import scala.io.Source.fromFile

class SlackNotificationServiceTest extends FunSuite {

  val doc = TestSolrDocuments("fatal")

  val path         = "alert-engine/scripts/slack-webhook-url.txt"
  val slackUrlFile = new File(path)

  val alertrule = TestAlertrules("slackWithRetryInterval10")
  val alertrule2 = TestAlertrules("slackWithRetryInterval20")

  val triggeredalert = TestTriggers("slackWithSparkAppAndTotalNumFound12")
  val triggeredalert2 = TestTriggers("slackWithPipeWrenchAppAndTotalNumFound14")

  test("sending a triggered alert to a slack profile") {
    if (slackUrlFile.exists) {
      val token        = fromFile(path).getLines.mkString
      val profile      = SlackAlertProfile("a", token)
      val slackService = new SlackNotificationService()
      slackService.notify(Seq(triggeredalert), profile)
    } else {
      println("no slack webhook url, skip the test")
    }
  }

  test("sending two triggered alerts to a slack profile") {
    if (slackUrlFile.exists) {
      val token        = fromFile(path).getLines.mkString
      val profile      = SlackAlertProfile("b", token)
      val slackService = new SlackNotificationService()
      slackService.notify(Seq(triggeredalert, triggeredalert2), profile)
    } else {
      println("no slack webhook url, skip the test")
    }
  }
}
