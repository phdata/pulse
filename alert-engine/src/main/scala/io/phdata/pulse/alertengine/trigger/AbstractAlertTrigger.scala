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

package io.phdata.pulse.alertengine.trigger

import com.typesafe.scalalogging.Logger
import io.phdata.pulse.alertengine.{ AlertRule, AlertsDb, TriggeredAlert }
import org.slf4j.LoggerFactory

/**
 * An abstract class for alert triggers that handles the logic of when to query and when to trigger
 * the alert. Children need only implement the query method to perform the query on the data source.
 */
abstract class AbstractAlertTrigger extends AlertTrigger {
  protected lazy val _logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  def logger: Logger = _logger

  /**
   * Queries using the given alert rule to determine if an alert should be triggered.
   * @param applicationName the application name
   * @param alertRule the alert rule
   * @return the matching documents if any
   */
  def query(applicationName: String, alertRule: AlertRule): Seq[Map[String, Any]]

  override final def check(applicationName: String, alertRule: AlertRule): Option[TriggeredAlert] =
    if (AlertsDb.shouldCheck(applicationName, alertRule)) {
      try {
        val results = query(applicationName, alertRule)
        processResults(applicationName, alertRule, results)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          logger.error(s"Error running query for $applicationName with alert $alertRule", e)
          None
      }
    } else {
      None
    }

  private def processResults(applicationName: String,
                             alertRule: AlertRule,
                             results: Seq[Map[String, Any]]): Option[TriggeredAlert] = {
    val numFound  = results.size
    val threshold = alertRule.resultThreshold.getOrElse(0)
    if (threshold == -1 && results.isEmpty) {
      logger.info(
        s"Alert triggered for $applicationName on alert $alertRule at no results found condition")
      AlertsDb.markTriggered(applicationName, alertRule)
      Some(TriggeredAlert(alertRule, applicationName, results, 0))
    } else if (results.lengthCompare(threshold) > 0) {
      logger.info(s"Alert triggered for $applicationName on alert $alertRule")
      AlertsDb.markTriggered(applicationName, alertRule)
      Some(TriggeredAlert(alertRule, applicationName, results, numFound))
    } else {
      logger.info(s"No alert needed for $applicationName with alert $alertRule")
      None
    }
  }
}
