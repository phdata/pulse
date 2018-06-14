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

import java.time.temporal.ChronoUnit
import java.time.{ ZoneOffset, ZonedDateTime }

import com.typesafe.scalalogging.LazyLogging

/**
 * Alerts DB keeps track of alert rules that have been checked
 */
object AlertsDb extends LazyLogging {
  private var checkedAlertRules: List[(String, AlertRule, ZonedDateTime)] = List()

  /**
   * Mark an alert checked, so it won't be queried again within a time period
   *
   * @param alert The Alert
   * @param now   A timestamp, visible for testing
   */
  def markTriggered(applicationName: String,
                    alert: AlertRule,
                    now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Unit =
    this.synchronized {
      // if the alert is already in the list, remove it
      val alertRemoved = checkedAlertRules.filterNot {
        case (storedAppName, storedAlert, _) =>
          storedAppName == applicationName && storedAlert == alert
      }

      checkedAlertRules = alertRemoved :+ (applicationName, alert, now)
    }

  /**
   * Test if we should check an alert rule again. If the alert is silenced (because it has alerted
   * recently) or we have recently checked this rule, the function will return <code>false</code>
   *
   * @param alertRule the alert rule
   * @param now       the time now
   * @return
   */
  def shouldCheck(applicationName: String,
                  alertRule: AlertRule,
                  now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Boolean = {
    val insideWaitPeriod = checkedAlertRules.exists {
      case (storedApplicationName, storedAlert, time) =>
        storedApplicationName == applicationName && alertRule == storedAlert && ChronoUnit.MINUTES
          .between(time, now) <= alertRule.retryInterval
    }

    !insideWaitPeriod
  }

  /**
   * Cleans the AlertsDb
   */
  def close(): Unit =
    clean()

  /**
   * Remove all old alert records.
   *
   * @param now the time now
   */
  def clean(now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Unit =
    this.synchronized {
      checkedAlertRules = checkedAlertRules.filter {
        case (_, storedAlert, alertedTime) =>
          ChronoUnit.MINUTES
            .between(alertedTime, now) <= storedAlert.retryInterval
      }
    }

  /**
   * Remove all records from the database. This should be used for testing only, calling this method
   * clears all data about when alerts were checked
   */
  def reset(): Unit =
    checkedAlertRules = List()
}
