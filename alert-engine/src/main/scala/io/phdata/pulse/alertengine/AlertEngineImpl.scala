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

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.alertengine.notification.NotificationServices
import io.phdata.pulse.common.JsonSupport
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.CloudSolrServer

import scala.collection.JavaConverters._

/**
 * The Alert Engine will
 * - check if each alert rule is triggered
 * - group similar alerts
 * - send alerts via notification services, like mail or slack
 *
 * @param solrServer          Solr service used to run queries against solr collections
 * @param notificatonServices Notification services used to send notifications, like mail or slack messages
 */
class AlertEngineImpl(solrServer: CloudSolrServer, notificatonServices: NotificationServices)
    extends AlertEngine
    with JsonSupport
    with LazyLogging {

  def run(applications: List[Application], silencedApplications: List[String]): Unit = {
    val activeApplications = filterSilencedApplications(applications, silencedApplications)
    val triggeredAlerts    = allTriggeredAlerts(activeApplications)
    val groupedAlerts      = groupTriggeredAlerts(triggeredAlerts)
    notify(groupedAlerts)
  }

  def filterSilencedApplications(applications: List[Application],
                                 silencedApplicationNames: List[String]): List[Application] = {
    val silencedApplications =
      applications.filter(app => silencedApplicationNames.contains(app.name))
    silencedApplications.foreach(app => logger.info(s"silencing application ${app.name}"))
    applications.filterNot(app => silencedApplications.contains(app))
  }

  def allTriggeredAlerts(
      applications: List[Application]): List[(Application, Option[TriggeredAlert])] =
    for (application <- applications;
         alertRule   <- application.alertRules)
      yield (application, triggeredAlert(application.name, alertRule))

  /**
   * Query solr for each alert rule to check if it should be triggered.
   * If the 'resultThreshold' is 0 or greater, trigger if a result is found.
   * If the 'resultThreshold' is less than 0, trigger if no documents are found.
   * @param applicationName The application name
   * @param alertRule       The alert rule to be checked
   * @return an Option of [[io.phdata.pulse.alertengine.TriggeredAlert]]
   */
  def triggeredAlert(applicationName: String, alertRule: AlertRule): Option[TriggeredAlert] =
    if (AlertsDb.shouldCheck(applicationName, alertRule)) {
      try {
        val alias = s"${applicationName}_all"
        val query = new SolrQuery(alertRule.query)
        query.set("fl", "*") // return full results
        query.set("collection", alias)
        val results    = solrServer.query(query).getResults
        val numFound   = results.getNumFound
        val resultsSeq = results.asScala
        val threshold  = alertRule.resultThreshold.getOrElse(0)
        if (threshold == -1 && results.isEmpty) {
          logger.info(
            s"Alert triggered for $applicationName on alert $alertRule at no results found condition")
          AlertsDb.markTriggered(applicationName, alertRule)
          Some(TriggeredAlert(alertRule, applicationName, resultsSeq, 0))
        } else if (resultsSeq.lengthCompare(threshold) > 0) {
          logger.info(s"Alert triggered for $applicationName on alert $alertRule")
          AlertsDb.markTriggered(applicationName, alertRule)
          Some(TriggeredAlert(alertRule, applicationName, resultsSeq, numFound))
        } else {
          logger.info(s"No alert needed for $applicationName with alert $alertRule")
          None
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error running query for alert $alertRule", e)
          None
      }
    } else {
      None
    }

  /**
   * Notify all [[io.phdata.pulse.alertengine.notification.NotificationService]] when an alert
   * is triggered.
   * @param groupedAlerts
   */
  def notify(
      groupedAlerts: Map[(Application, String), Seq[(Application, TriggeredAlert, String)]]): Unit =
    groupedAlerts.foreach {
      case (grouping, alertApp) =>
        sendAlert(grouping._1, grouping._2, alertApp.map(_._2))
    }

  /**
   * Notify based on triggered alert.
   * @param application The application configuration
   * @param profileName The profile name
   * @param alerts      A list of alerts that need to be sent
   */
  def sendAlert(application: Application,
                profileName: String,
                alerts: Iterable[TriggeredAlert]): Unit = {
    logger.debug(s"sending alerts $alerts")
    logger.info(s"sending ${alerts.size} alerts")

    val mailProfiles: List[MailAlertProfile] =
      getMatchingProfiles(profileName, application.emailProfiles)
    val slackProfiles: List[SlackAlertProfile] =
      getMatchingProfiles(profileName, application.slackProfiles)

    if (!(mailProfiles.nonEmpty || slackProfiles.nonEmpty)) {
      throw new IllegalStateException("No matching alert profiles found. Nothing to notify.")
    }

    logger.info(s"found profiles $mailProfiles, $slackProfiles")

    for (profile <- mailProfiles) {
      notificatonServices.mailService.notify(alerts, profile)
    }

    for (profile <- slackProfiles) {
      notificatonServices.slackService.notify(alerts, profile)
    }
  }

  def getMatchingProfiles[A <: AlertProfile](profileName: String,
                                             maybeProfiles: Option[List[A]]): List[A] =
    maybeProfiles
      .map(profiles =>
        profiles.collect {
          case p if p.name == profileName => p
      })
      .getOrElse(List())

  /**
   * Group like alerts by severity level and profile
   *
   * @param triggeredAlerts Alerts that have been triggered
   * @return Alerts grouped by severity and profile
   */
  def groupTriggeredAlerts(triggeredAlerts: List[(Application, Option[TriggeredAlert])])
    : Map[(Application, String), Seq[(Application, TriggeredAlert, String)]] = {
    // get all Some(TriggeredAlert)
    val flattenedTriggeredAlerts: Seq[(Application, TriggeredAlert)] = triggeredAlerts.collect {
      case (application, Some(alertOption)) =>
        (application, alertOption)
    }

    // join in the profiles with each application/alert
    val withProfiles = for (application_alert <- flattenedTriggeredAlerts;
                            profile <- application_alert._2.rule.alertProfiles)
      yield (application_alert._1, application_alert._2, profile)

    // group by the application and profiles
    withProfiles.groupBy {
      case (application, alert, profile) => (application, profile)
    }
  }

}
