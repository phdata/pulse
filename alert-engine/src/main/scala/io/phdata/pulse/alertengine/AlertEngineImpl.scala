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

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.alertengine.notification.NotificationServices
import io.phdata.pulse.alertengine.trigger.{ AlertTrigger, SolrAlertTrigger, SqlAlertTrigger }

class AlertEngineImpl(solrTrigger: Option[SolrAlertTrigger],
                      sqlTrigger: Option[SqlAlertTrigger],
                      val notificationServices: NotificationServices)
    extends AlertEngine
    with LazyLogging {

  if (solrTrigger.isEmpty && sqlTrigger.isEmpty) {
    throw new IllegalStateException("At least one alert trigger must be provided")
  }

  /**
   * Run the AlertEngine.
   *
   * @param applications         List of applications to alert on
   * @param silencedApplications List of applications that have been silenced and should not alert
   */
  override def run(applications: List[Application], silencedApplications: List[String]): Unit = {
    val activeApplications = filterSilencedApplications(applications, silencedApplications)
    val triggeredAlerts    = allTriggeredAlerts(activeApplications)
    val groupedAlerts      = groupTriggeredAlerts(triggeredAlerts)
    notify(groupedAlerts)
  }

  /**
   * Notify all [[io.phdata.pulse.alertengine.notification.NotificationService]] when an alert
   * is triggered.
   *
   * @param groupedAlerts the triggered alerts grouped by application and profile
   */
  def notify(
      groupedAlerts: Map[(Application, String), Seq[(Application, TriggeredAlert, String)]]): Unit =
    groupedAlerts.foreach {
      case (grouping, alertApp) =>
        sendAlert(grouping._1, grouping._2, alertApp.map(_._2))
    }

  /**
   * Query for each alert rule to check if it should be triggered.
   * If the 'resultThreshold' is 0 or greater, trigger if a result is found.
   * If the 'resultThreshold' is less than 0, trigger if no documents are found.
   *
   * @param applicationName The application name
   * @param alertRule       The alert rule to be checked
   * @return an Option of [[io.phdata.pulse.alertengine.TriggeredAlert]]
   */
  def triggeredAlert(applicationName: String, alertRule: AlertRule): Option[TriggeredAlert] =
    alertRule.alertType match {
      case Some(AlertTypes.SOLR) => triggerOrElse(solrTrigger, applicationName, alertRule)
      case Some(AlertTypes.SQL)  => triggerOrElse(sqlTrigger, applicationName, alertRule)
      case None                  => triggerOrElse(solrTrigger, applicationName, alertRule)
      case _ =>
        logger.warn(s"Unknown alert type in alert rule: $alertRule")
        None
    }

  private def triggerOrElse(trigger: Option[AlertTrigger],
                            applicationName: String,
                            alertRule: AlertRule): Option[TriggeredAlert] =
    trigger.map(_.check(applicationName, alertRule)).getOrElse {
      logger.error(
        s"No ${alertRule.alertType} alert trigger available to process the alert rule: $alertRule")
      None
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
   * Notify based on triggered alert.
   *
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
      notificationServices.mailService.notify(alerts, profile)
    }

    for (profile <- slackProfiles) {
      notificationServices.slackService.notify(alerts, profile)
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
    val flattenedTriggeredAlerts = triggeredAlerts.collect {
      case (application, Some(alert)) =>
        (application, alert)
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
