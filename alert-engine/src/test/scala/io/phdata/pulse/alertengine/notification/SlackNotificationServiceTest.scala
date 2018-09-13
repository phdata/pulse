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

import io.phdata.pulse.alertengine.TestObjectGenerator
import org.apache.solr.common.SolrDocument
import org.scalatest.FunSuite

import scala.io.Source.fromFile

class SlackNotificationServiceTest extends FunSuite {
  val doc: SolrDocument = TestObjectGenerator.solrDocument(threadName = "OXB Thread")

  val path         = "alert-engine/scripts/slack-webhook-url.txt"
  val slackUrlFile = new File(path)

  val alertrule = TestObjectGenerator.alertRule()
  val alertrule2 = TestObjectGenerator.alertRule(retryInterval = 20)

  val triggeredalert = TestObjectGenerator.triggeredAlert(totalNumFound = 12)
  val triggeredalert2 = TestObjectGenerator.triggeredAlert(totalNumFound = 14)

  test("sending a triggered alert to a slack profile") {
    if (slackUrlFile.exists) {
      val token        = fromFile(path).getLines.mkString
      val profile = TestObjectGenerator.slackAlertProfile(name = "a", url = token)
      val slackService = new SlackNotificationService()
      slackService.notify(Seq(triggeredalert), profile)
    } else {
      println("no slack webhook url, skip the test")
    }
  }

  test("sending two triggered alerts to a slack profile") {
    if (slackUrlFile.exists) {
      val token        = fromFile(path).getLines.mkString
      val profile = TestObjectGenerator.slackAlertProfile(name = "b", url = token)
      val slackService = new SlackNotificationService()
      slackService.notify(Seq(triggeredalert, triggeredalert2), profile)
    } else {
      println("no slack webhook url, skip the test")
    }
  }
}
