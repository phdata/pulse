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

package io.phdata.pulse.alertengine

import java.io.File

import io.phdata.pulse.alertengine.notification.{
  MailNotificationService,
  NotificationServiceFactory,
  SlackNotificationService
}
import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.common.{ DocumentConversion, SolrService }
import io.phdata.pulse.testcommon.{ BaseSolrCloudTest, TestUtil }
import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.mockito.{ Matchers, Mockito }
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar

class AlertEngineImplTest extends FunSuite with BaseSolrCloudTest with MockitoSugar {
  val CONF_NAME       = "testconf"
  val TEST_COLLECTION = TestUtil.randomIdentifier()

  val solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

  val mailNotificationService =
    mock[MailNotificationService]

  val slackNotificationService =
    mock[SlackNotificationService]

  val mockSolrServer = mock[CloudSolrServer]

  override def beforeAll(): Unit = {
    super.beforeAll()
    val alias = TEST_COLLECTION + "_all"
    solrService.uploadConfDir(
      new File(System.getProperty("user.dir") + "/test-config/solr_configs/conf").toPath,
      CONF_NAME)

    solrClient.setDefaultCollection(alias)
    val result = solrService.createCollection(alias, 1, 1, CONF_NAME, null)

    val document = DocumentConversion.toSolrDocument(
      LogEvent(Some("id"),
               "ERROR",
               "1970-01-01T00:00:00Z",
               "ERROR",
               "message",
               "thread oxb",
               Some("Exception in thread main"),
               None))

    val document2 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id2"),
               "ERROR",
               "1972-01-01T22:00:00Z",
               "ERROR2",
               "message2",
               "thread2 oxb",
               Some("Exception in thread main2"),
               None))

    val document3 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id3"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document4 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id4"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document5 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id5"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document6 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id6"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document7 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id7"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document8 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id8"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document9 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id9"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document10 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id10"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document11 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id11"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    val document12 = DocumentConversion.toSolrDocument(
      LogEvent(Some("id12"),
               "ERROR",
               "1973-01-01T00:00:33Z",
               "ERROR",
               "message3",
               "thread3 oxb",
               Some("Exception in thread main3"),
               None))

    solrClient.add(document)
    solrClient.add(document2)
    solrClient.add(document3)
    solrClient.add(document4)
    solrClient.add(document5)
    solrClient.add(document6)
    solrClient.add(document7)
    solrClient.add(document8)
    solrClient.add(document9)
    solrClient.add(document10)
    solrClient.add(document11)
    solrClient.add(document12)

    solrClient.commit(true, true, true)
    // unset the default collection so we are sure it is being set in the request
    solrClient.setDefaultCollection("")
  }

  test("get active alert") {
    val alert = AlertRule("category: ERROR", 1, Some(0), List("tony@phdata.io"))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServiceFactory(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(TEST_COLLECTION, alert).get
    assertResult(alert)(result.rule)
    // @TODO why is this 2 instead of 1 sometimes?
    assert(result.documents.lengthCompare(0) > 0)
    assert(result.applicationName == TEST_COLLECTION)
  }

  test("trigger alert when threshold is set to '-1' and there are no results") {
    val alert = AlertRule("category: DOESNOTEXIST", 1, Some(-1), List("tony@phdata.io"))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServiceFactory(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(TEST_COLLECTION, alert).get
    assertResult(alert)(result.rule)

    assert(result.documents.isEmpty)
    assert(result.applicationName == TEST_COLLECTION)
  }

  test("don't match non alert") {
    val alert = AlertRule("id: notexists", 1, Some(0), List("tony@phdata.io"))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServiceFactory(mailNotificationService, slackNotificationService))
    assertResult(None)(engine.triggeredAlert(TEST_COLLECTION, alert))
  }

  test("Mail profile is matched from AlertRule to Application") {
    val mailAlertProfile = MailAlertProfile("mailprofile1", List("person@phdata.io"))

    val alertrule = AlertRule("id: id", 1, Some(0), List("mailprofile1"))
    val engine =
      new AlertEngineImpl(null, new NotificationServiceFactory(mailNotificationService, null))

    val triggeredalert  = TriggeredAlert(alertrule, "Spark", null, 1)
    val app             = Application("a", List(alertrule), Some(List(mailAlertProfile)), None)
    val triggeredAlerts = List(triggeredalert)

    engine.sendAlert(app, "mailprofile1", triggeredAlerts)

    Mockito.verify(mailNotificationService).notify(triggeredAlerts, mailAlertProfile)
  }

  test("Slack profile is matched from AlertRule to Application") {
    val profileName       = "slackProfile1"
    val slackAlertProfile = SlackAlertProfile(profileName, "https://slack.com")

    val alertrule = AlertRule("id: id", 1, Some(0), List(profileName))
    val engine =
      new AlertEngineImpl(null, new NotificationServiceFactory(null, slackNotificationService))

    val triggeredalert  = TriggeredAlert(alertrule, "Spark", null, 1)
    val app             = Application("a", List(alertrule), None, Some(List(slackAlertProfile)))
    val triggeredAlerts = List(triggeredalert)

    engine.sendAlert(app, profileName, triggeredAlerts)

    Mockito.verify(slackNotificationService).notify(triggeredAlerts, slackAlertProfile)
  }

  test("test groupTriggeredAlerts") {
    val engine =
      new AlertEngineImpl(
        null,
        new NotificationServiceFactory(mailNotificationService, slackNotificationService))

    val yaml =
      """---
        |applications:
        |- name: spark1
        |  alertRules:
        |  - query: "query"
        |    retryInterval: 10
        |    resultThreshold: 0
        |    alertProfiles:
        |    - mailProfile1
        |  - query: "query"
        |    retryInterval: 10
        |    resultThreshold: 0
        |    alertProfiles:
        |    - mailProfile1
        |  emailProfiles:
        |  - name: mailProfile1
        |    addresses:
        |    - test@phdata.io
        |  """.stripMargin
    val app = AlertEngineConfigParser.convert(yaml).applications.head

    val triggeredAlerts = List((app, Option(TriggeredAlert(app.alertRules(0), "spark1", null, 1))),
                               (app, Option(TriggeredAlert(app.alertRules(1), "spark2", null, 2))))

    val groupedTriggerdAlerts = engine.groupTriggeredAlerts(triggeredAlerts)

    assert(groupedTriggerdAlerts.size == 1)
  }

  test("Silenced applications alerts aren't checked") {
    val mailAlertProfile = MailAlertProfile("mailprofile1", List("person@phdata.io"))

    val alertrule = AlertRule("id: id", 1, Some(0), List("mailprofile1"))

    val activeApp        = Application("activeApp", List(alertrule), None, None)
    val silencedApp      = Application("silencedApp", List(alertrule), None, None)
    val silencedAppNames = List("silencedApp")

    val engine =
      new AlertEngineImpl(null, null)

    val results = engine.filterSilencedApplications(List(activeApp, silencedApp), silencedAppNames)
    println(results)
    assert(results.length == 1)
    assert(results.contains(activeApp))
  }
}
