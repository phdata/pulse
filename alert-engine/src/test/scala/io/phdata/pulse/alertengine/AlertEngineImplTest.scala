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

    val document = DocumentConversion.toSolrDocument(TestObjectGenerator.logEvent())
    val documentError = DocumentConversion.toSolrDocument(TestObjectGenerator.logEvent(id = None, category = "ERROR", level = "ERROR2", message = "message2", throwable = Some("Exception in thread main")))

    solrClient.add(document)

    for (i <- 1 to 12) {
      solrClient.add(documentError)
    }

    solrClient.commit(true, true, true)
    // unset the default collection so we are sure it is being set in the request
    solrClient.setDefaultCollection("")
  }

  test("get active alert") {
    val a = TestObjectGenerator.alertRule(query = "category: ERROR", retryInterval = 1, resultThreshold = Some(1), alertProfiles = List("test@phdata.io"))

    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(APPLICATION_NAME, a).get
    assertResult(a)(result.rule)
    // @TODO why is this 2 instead of 1 sometimes?
    assert(result.documents.lengthCompare(0) > 0)
    assert(result.applicationName == APPLICATION_NAME)
    assert(result.totalNumFound == 12)
  }

  test("trigger alert when threshold is set to '-1' and there are no results") {
    val alert = TestObjectGenerator.alertRule(resultThreshold = Some(-1))
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
    val alert = TestObjectGenerator.alertRule()
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assertResult(None)(engine.triggeredAlert(APPLICATION_NAME, alert))
  }

  test("Mail profile is matched from AlertRule to Application") {
    val mailAlertProfile = TestObjectGenerator.mailAlertProfile()

    val alertrule = TestObjectGenerator.alertRule()
    val engine =
      new AlertEngineImpl(null, new NotificationServices(mailNotificationService, null))

    val triggeredalert = TestObjectGenerator.triggeredAlert(documents = null)
    val app             = Application("a", List(alertrule), Some(List(mailAlertProfile)), None)
    val triggeredAlerts = List(triggeredalert)

    engine.sendAlert(app, "mailprofile1", triggeredAlerts)

    Mockito.verify(mailNotificationService).notify(triggeredAlerts, mailAlertProfile)
  }

  test("Slack profile is matched from AlertRule to Application") {
    val profileName       = "slackProfile1"
    val slackAlertProfile = TestObjectGenerator.slackAlertProfile(name = profileName, url = "https://slack.com")

    val alertrule = TestObjectGenerator.alertRule()
    val engine =
      new AlertEngineImpl(null, new NotificationServices(null, slackNotificationService))

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
        new NotificationServices(mailNotificationService, slackNotificationService))

    val alertrule = TestObjectGenerator.alertRule(query = "spark1query")
    val alertrule1 = TestObjectGenerator.alertRule(query = "spark2query1")
    val alertrule2 = TestObjectGenerator.alertRule(query = "spark2query2")
    val App1       = Application("spark1", List(alertrule), None, None)
    val App2       = Application("spark2", List(alertrule1, alertrule2), None, None)

    val triggeredAlerts =
      List(
        (App1, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark"))),
        (App2, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark1"))),
        (App2, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2")))
      )

    val groupedTriggerdAlerts =
      engine.groupTriggeredAlerts(triggeredAlerts)

    assert(groupedTriggerdAlerts.size == 2)
    assert(groupedTriggerdAlerts.head._2.size == 1)
    assert(groupedTriggerdAlerts.tail.head._2.size == 2)

  }

  test("Silenced applications alerts aren't checked") {
    val mailAlertProfile = TestObjectGenerator.mailAlertProfile()

    val alertrule = TestObjectGenerator.alertRule()

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
    val alert = TestObjectGenerator.alertRule(query = "category: ERROR", retryInterval = 1, alertProfiles = List("testing@tester.com"))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    val result = engine.triggeredAlert(APPLICATION_NAME, alert).get
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("mark alert triggered when results are found") {
    val alert = TestObjectGenerator.alertRule(query = "category: ERROR", retryInterval = 1)
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assert(engine.triggeredAlert(APPLICATION_NAME, alert).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("mark alert triggered when no results are found") {
    val alert = TestObjectGenerator.alertRule(resultThreshold = Some(-1))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assert(engine.triggeredAlert(APPLICATION_NAME, alert).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }

  test("don't mark alert triggered when no results are found") {
    val alert = TestObjectGenerator.alertRule(resultThreshold = Some(1))
    val engine =
      new AlertEngineImpl(
        solrClient,
        new NotificationServices(mailNotificationService, slackNotificationService))
    assertResult(None)(engine.triggeredAlert(APPLICATION_NAME, alert))
    assertResult(true)(AlertsDb.shouldCheck(APPLICATION_NAME, alert))
  }
}
