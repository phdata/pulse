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

import io.phdata.pulse.alertengine.notification.{MailNotificationService, NotificationServices, SlackNotificationService}
import io.phdata.pulse.common.{DocumentConversion, SolrService}
import io.phdata.pulse.testcommon.{BaseSolrCloudTest, TestUtil}
import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuite}

class AlertEngineImplTest
    extends FunSuite
    with BaseSolrCloudTest
    with MockitoSugar
    with BeforeAndAfterEach {
  val CONF_NAME        = "testconf"
  val APPLICATION_NAME = TestUtil.randomIdentifier()

  val solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

  val mailNotificationService =
    mock[MailNotificationService]

  val slackNotificationService =
    mock[SlackNotificationService]

  val mockSolrServer = mock[CloudSolrServer]

  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    AlertsDb.reset()
    val alias = APPLICATION_NAME + "_all"
    solrService.uploadConfDir(
      new File(System.getProperty("user.dir") + "/test-config/solr_configs/conf").toPath,
      CONF_NAME)

    solrClient.setDefaultCollection(alias)
    val result = solrService.createCollection(alias, 1, 1, CONF_NAME, null)

    val document = DocumentConversion.toSolrDocument(TestLogEvents("alertError"))
    val documentError = DocumentConversion.toSolrDocument(TestLogEvents("errorError2"))


    solrClient.add(document)

    for (i <- 1 to 12) {
      solrClient.add(documentError)
    }

    solrClient.commit(true, true, true)
    // unset the default collection so we are sure it is being set in the request
    solrClient.setDefaultCollection("")
  }

  test("get active alert") {
    val alert = AlertRule("category: ERROR", 1, Some(0), List("tony@phdata.io"))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(APPLICATION_NAME, alert).get
    assertResult(alert)(result.rule)
    // @TODO why is this 2 instead of 1 sometimes?
    assert(result.documents.lengthCompare(0) > 0)
    assert(result.applicationName == APPLICATION_NAME)
    assert(result.totalNumFound == 12)
  }

  test("trigger alert when threshold is set to '-1' and there are no results") {
    val alert = TestAlertrules("emailWithRetryInterval1")
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(APPLICATION_NAME, alert).get
    assertResult(alert)(result.rule)
    assert(result.documents.isEmpty)
    assert(result.applicationName == APPLICATION_NAME)
  }

  test("don't match non alert") {
    val alert = TestAlertrules("emailWithRetryInterval1AndQueryIsNotexists")
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assertResult(None)(engine.triggeredAlert(APPLICATION_NAME, alert))
  }

  test("Mail profile is matched from AlertRule to Application") {
    val mailAlertProfile = TestMailAlertProfiles("withAddressMailProfile1")

    val alertrule = AlertRule("id: id", 1, Some(0), List("mailprofile1"))
    val engine =
      new AlertEngineImpl(null, new NotificationServices(mailNotificationService, null))

    val triggeredalert  = TriggeredAlert(alertrule, "Spark", null, 1)
    val app             = Application("a", List(alertrule), Some(List(mailAlertProfile)), None)
    val triggeredAlerts = List(triggeredalert)

    engine.sendAlert(app, "mailprofile1", triggeredAlerts)

    Mockito.verify(mailNotificationService).notify(triggeredAlerts, mailAlertProfile)
  }

  test("Slack profile is matched from AlertRule to Application") {
    val profileName       = "slackProfile1"
    val slackAlertProfile = SlackAlertProfile(profileName, "https://slack.com")

    val alertrule = TestAlertrules("emailWithRetryInterval1AndQueryIsId")
    val engine =
      new AlertEngineImpl(null, new NotificationServices(null, slackNotificationService))

    val triggeredalert = TestTriggers("slackWithSparkAppWithNullDocAndTotalNumFound1")

    val app             = Application("a", List(alertrule), None, Some(List(slackAlertProfile)))
    val triggeredAlerts = List(triggeredalert)

    engine.sendAlert(app, profileName, triggeredAlerts)

    Mockito.verify(slackNotificationService).notify(triggeredAlerts, slackAlertProfile)
  }

  test("test groupTriggeredAlerts") {
    val engine =
      new AlertEngineImpl(
        null,
        new NotificationServices(mailNotificationService, slackNotificationService))


    val alertrule = TestAlertrules("emailWithRetryInterval1AndQueryIsSpark1query")
    val alertrule1 = TestAlertrules("emailWithRetryInterval1AndQueryIsSpark1query1")
    val alertrule2 = TestAlertrules("emailWithRetryInterval1AndQueryIsSpark1query1")


    val App1       = Application("spark1", List(alertrule), None, None)
    val App2       = Application("spark2", List(alertrule1, alertrule2), None, None)

    val triggeredAlerts =
      List(
        (App1, Option(TriggeredAlert(alertrule, "spark1", null, 2))),
        (App2, Option(TriggeredAlert(alertrule1, "spark2", null, 2))),
        (App2, Option(TriggeredAlert(alertrule2, "spark2", null, 2)))
      )

    val groupedTriggerdAlerts =
      engine.groupTriggeredAlerts(triggeredAlerts)

    assert(groupedTriggerdAlerts.size == 2)
    assert(groupedTriggerdAlerts.head._2.size == 1)
    assert(groupedTriggerdAlerts.tail.head._2.size == 2)

  }

  test("Silenced applications alerts aren't checked") {
    val mailAlertProfile = TestMailAlertProfiles("withAddressMailProfile1")

    val alertrule = TestAlertrules("emailWithRetryInterval1AndQueryIsSpark1query")

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

  test("mark alert triggered on results found > 0") {
    val alert = TestAlertrules("emailWithRetryInterval1")
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(APPLICATION_NAME, alert).get
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("mark alert triggered when results are found") {
    val alert = TestAlertrules("emailWithRetryInterval1")

    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assert(engine.triggeredAlert(APPLICATION_NAME, alert).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("mark alert triggered when no results are found") {
    val alert = TestAlertrules("emailWithRetryInterval1")

    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assert(engine.triggeredAlert(APPLICATION_NAME, alert).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("don't mark alert triggered when no results are found") {
    val alert = TestAlertrules("emailWithRetryInterval1AndresultThreshold1")

    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assertResult(None)(engine.triggeredAlert(APPLICATION_NAME, alert))
    assertResult(true)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }
}
