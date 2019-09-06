/*
 * Copyright 2019 phData Inc.
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

import com.typesafe.scalalogging.Logger
import io.phdata.pulse.alertengine.notification.{MailNotificationService, NotificationServices, SlackNotificationService}
import io.phdata.pulse.alertengine.trigger.{SolrAlertTrigger, SqlAlertTrigger}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.LoggerFactory

class AlertEngineImplTest extends FunSuite with MockitoSugar with BeforeAndAfterEach {

  val mailNotificationService: MailNotificationService = mock[MailNotificationService]
  val slackNotificationService: SlackNotificationService = mock[SlackNotificationService]
  lazy val notificationServices: NotificationServices =
    new NotificationServices(mailNotificationService, slackNotificationService)

  val solrAlertTrigger: SolrAlertTrigger = mock[SolrAlertTrigger]
  val sqlAlertTrigger: SqlAlertTrigger = mock[SqlAlertTrigger]

  val engine = new AlertEngineImpl(Some(solrAlertTrigger), Some(sqlAlertTrigger), notificationServices)

  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
    reset(solrAlertTrigger, sqlAlertTrigger, mailNotificationService, slackNotificationService)

    when(solrAlertTrigger.query(Matchers.anyString, Matchers.any[AlertRule])).thenReturn(Seq.empty)
    when(solrAlertTrigger.logger).thenReturn(Logger(LoggerFactory.getLogger(solrAlertTrigger.getClass)))
    when(sqlAlertTrigger.query(Matchers.anyString, Matchers.any[AlertRule])).thenReturn(Seq.empty)
    when(sqlAlertTrigger.logger).thenReturn(Logger(LoggerFactory.getLogger(sqlAlertTrigger.getClass)))
  }

  test("Mail profile is matched from AlertRule to Application") {
    val mailAlertProfile = TestObjectGenerator.mailAlertProfile()

    val alertRule = TestObjectGenerator.alertRule()

    val triggeredAlert = TestObjectGenerator.triggeredAlert(documents = Seq.empty)
    val app = Application("testApp", List(alertRule), Some(List(mailAlertProfile)), None)
    val triggeredAlerts = List(triggeredAlert)

    engine.sendAlert(app, "mailprofile1", triggeredAlerts)

    Mockito.verify(mailNotificationService).notify(triggeredAlerts, mailAlertProfile)
  }

  test("Slack profile is matched from AlertRule to Application") {
    val profileName = "slackProfile1"
    val slackAlertProfile =
      TestObjectGenerator.slackAlertProfile(name = profileName, url = "https://slack.com")

    val alertRule = TestObjectGenerator.alertRule()

    val triggeredAlert = TriggeredAlert(alertRule, "Spark", Seq.empty, 1)
    val app = Application("testApp", List(alertRule), None, Some(List(slackAlertProfile)))
    val triggeredAlerts = List(triggeredAlert)

    engine.sendAlert(app, profileName, triggeredAlerts)

    Mockito.verify(slackNotificationService).notify(triggeredAlerts, slackAlertProfile)
  }

  test("test groupTriggeredAlerts") {
    val alertRule = TestObjectGenerator.alertRule(query = "spark1query")
    val alertRule1 = TestObjectGenerator.alertRule(query = "spark2query1")
    val alertRule2 = TestObjectGenerator.alertRule(query = "spark2query2")
    val app1 = Application("spark1", List(alertRule), None, None)
    val app2 = Application("spark2", List(alertRule1, alertRule2), None, None)

    val triggeredAlerts =
      List(
        (app1, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark"))),
        (app2, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark1"))),
        (app2, Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2")))
      )

    val groupedTriggerdAlerts =
      engine.groupTriggeredAlerts(triggeredAlerts)

    assert(groupedTriggerdAlerts.size == 2)
    assert(groupedTriggerdAlerts.head._2.size == 1)
    assert(groupedTriggerdAlerts.tail.head._2.size == 2)
  }

  test("Silenced applications alerts aren't checked") {
    val mailAlertProfile = TestObjectGenerator.mailAlertProfile()

    val alertRule = TestObjectGenerator.alertRule()

    val activeApp = Application("activeApp", List(alertRule), None, None)
    val silencedApp = Application("silencedApp", List(alertRule), None, None)
    val silencedAppNames = List("silencedApp")

    val results = engine.filterSilencedApplications(List(activeApp, silencedApp), silencedAppNames)
    println(results)
    assert(results.length == 1)
    assert(results.contains(activeApp))
  }

  test("test allTriggeredAlerts") {
    val alertRule = TestObjectGenerator.alertRule(query = "spark1query", alertType = Some(AlertTypes.SOLR))
    val alertRule1 = TestObjectGenerator.alertRule(query = "spark2query1", alertType=Some(AlertTypes.SOLR))
    val alertRule2 = TestObjectGenerator.alertRule(query = "spark2query2", alertType = Some(AlertTypes.SOLR))
    val app1 = Application("spark1", List(alertRule), None, None)
    val app2 = Application("spark2", List(alertRule1, alertRule2), None, None)
    val docs = Seq[Map[String, Any]](Map.empty, Map.empty)
    when(solrAlertTrigger.query(Matchers.anyString(), Matchers.any[AlertRule])).thenReturn(docs)

    val alert1 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark1", rule = alertRule, totalNumFound = 2, documents = docs))
    val alert2 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2", rule = alertRule1, totalNumFound = 2, documents = docs))
    val alert3 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2", rule = alertRule2, totalNumFound = 2, documents = docs))

    val expected =
      List(
        (app1, alert1),
        (app2, alert2),
        (app2, alert3)
      )

    val results = engine.allTriggeredAlerts(List(app1, app2))
    assertResult(expected)(results)
  }

  test("test run") {
    val alertRule1 =
      TestObjectGenerator.alertRule(query = "spark1query", alertProfiles = List("email"))
    val alertRule2 =
      TestObjectGenerator.alertRule(query = "spark2query1", alertProfiles = List("slack"))
    val alertRule3 =
      TestObjectGenerator.alertRule(query = "spark2query2", alertProfiles = List("slack"))
    val alertRule4 =
      TestObjectGenerator.alertRule(query = "spark3query1", alertProfiles = List("unused"))

    val docs = Seq[Map[String, Any]](Map.empty, Map.empty)
    when(solrAlertTrigger.query(Matchers.anyString(), Matchers.any[AlertRule])).thenReturn(docs)

    val alert1 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark1", rule = alertRule1, totalNumFound = 2, documents = docs))
    val alert2 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2", rule = alertRule2, totalNumFound = 2, documents = docs))
    val alert3 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark2", rule = alertRule3, totalNumFound = 2, documents = docs))
    val alert4 =
      Option(TestObjectGenerator.triggeredAlert(applicationName = "spark3", rule = alertRule4, totalNumFound = 2, documents = docs))

    val mailAlertProfile = TestObjectGenerator.mailAlertProfile(name = "email")
    val slackAlertProfile = TestObjectGenerator.slackAlertProfile(name = "slack")
    val app1 = Application("spark1", List(alertRule1), Some(List(mailAlertProfile)), None)
    val app2 =
      Application("spark2", List(alertRule2, alertRule3), None, Some(List(slackAlertProfile)))
    val app3 = Application("spark3", List(alertRule4), None, None)

    engine.run(List(app1, app3, app2), List(app3.name))

    Mockito.verify(mailNotificationService).notify(List(alert1.get), mailAlertProfile)
    Mockito.verify(slackNotificationService).notify(List(alert2.get, alert3.get), slackAlertProfile)
  }

  test("SolrAlertTrigger called") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
      retryInterval = 1,
      resultThreshold = Some(1),
      alertProfiles = List("test@phdata.io"),
      Some(AlertTypes.SOLR))
    val applicationName = "some-name"
    val docs = Seq[Map[String, Any]](Map.empty, Map.empty)
    val expected =
      Some(TestObjectGenerator.triggeredAlert(applicationName = applicationName, rule = alertRule, totalNumFound = docs.length, documents =docs ))
    when(solrAlertTrigger.query(applicationName, alertRule)).thenReturn(docs)

    val result = engine.triggeredAlert(applicationName, alertRule)
    assertResult(expected)(result)
    verify(solrAlertTrigger).query(applicationName, alertRule)
    verifyZeroInteractions(sqlAlertTrigger)
  }

  test("SolrAlertTrigger called when no alert type is defined") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
      retryInterval = 1,
      resultThreshold = Some(1),
      alertProfiles = List("test@phdata.io"),
      None)
    val applicationName = "some-name"
    val docs = Seq[Map[String, Any]](Map.empty, Map.empty)
    val expected =
      Some(TestObjectGenerator.triggeredAlert(applicationName = applicationName, rule = alertRule, totalNumFound = docs.length, documents = docs))
    when(solrAlertTrigger.query(applicationName, alertRule)).thenReturn(docs)

    val result = engine.triggeredAlert(applicationName, alertRule)
    assertResult(expected)(result)
    verify(solrAlertTrigger).query(applicationName, alertRule)
    verifyZeroInteractions(sqlAlertTrigger)
  }

  test("SqlAlertTrigger called") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
      retryInterval = 1,
      resultThreshold = Some(1),
      alertProfiles = List("test@phdata.io"),
      Some(AlertTypes.SQL))
    val applicationName = "some-name"
    val docs = Seq[Map[String, Any]](Map.empty, Map.empty)
    val expected =
      Some(TestObjectGenerator.triggeredAlert(applicationName = applicationName, rule = alertRule, totalNumFound = docs.length, documents = docs))
    when(sqlAlertTrigger.query(applicationName, alertRule)).thenReturn(docs)

    val result = engine.triggeredAlert(applicationName, alertRule)
    assertResult(expected)(result)
    verify(sqlAlertTrigger).query(applicationName, alertRule)
    verifyZeroInteractions(solrAlertTrigger)
  }

  test("No matching alert trigger") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
      retryInterval = 1,
      resultThreshold = Some(1),
      alertProfiles = List("test@phdata.io"),
      Some("blah"))

    val result = engine.triggeredAlert("hi", alertRule)
    verifyZeroInteractions(solrAlertTrigger)
    verifyZeroInteractions(sqlAlertTrigger)
  }

  test("No alert triggers provided") {
    assertThrows[IllegalStateException](new AlertEngineImpl(None, None, notificationServices))
  }

}
