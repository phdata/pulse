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

import java.io._
import java.time.temporal.ChronoUnit
import java.time.{ ZoneOffset, ZonedDateTime }
import com.typesafe.scalalogging.LazyLogging

/**
 * Alerts DB keeps track of alert rules that have been checked
 */
object AlertsDb extends LazyLogging {

  /**
   * Not using the class ObjectInputStreamWithCustomClassLoader will give the below exception -
   * "java.lang.ClassCastException: cannot assign instance of scala.collection.immutable.List$SerializationProxy to
   * field io.phdata.pulse.engine.AlertsDb$AlertsDbHolder.checkedAlertRules of type scala.collection.immutable.List in
   * instance of io.phdata.pulse.engine.AlertsDb$AlertsDbHolder"
   * Ref - https://gist.github.com/ramn/5566596
   */
  class ObjectInputStreamWithCustomClassLoader(fileInputStream: FileInputStream)
      extends ObjectInputStream(fileInputStream) {
    override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] =
      try { Class.forName(desc.getName, false, getClass.getClassLoader) } catch {
        case ex: ClassNotFoundException => super.resolveClass(desc)
      }
  }

  case class AlertsDbHolder(var checkedAlertRules: List[(AlertRule, ZonedDateTime)])
      extends Serializable

  val path                                                        = "/tmp/alertsDb.ser"
  private var checkedAlertRules: List[(AlertRule, ZonedDateTime)] = List()

  def load(): Unit = {
    val file = new File(path)
    if (file.exists()) {
      val br = new BufferedReader(new FileReader(path))
      try {
        // if the file is not empty
        if (br.readLine() != null) {
          // reads from the file and deserializes
          val fis = new FileInputStream(file)
          val ois = new ObjectInputStreamWithCustomClassLoader(fis)

          val obj = ois.readObject.asInstanceOf[AlertsDbHolder]
          ois.close()

          AlertsDb.checkedAlertRules = obj.checkedAlertRules
        }
      } catch {
        case e: Exception => logger.error(s"Error loading AlertsDb from path - $path ", e)
      } finally {
        br.close()
      }
    }
  }

  def persist(): Unit = {
    val obj = AlertsDbHolder(List())
    obj.checkedAlertRules = AlertsDb.checkedAlertRules
    try {
      // serializes and writes to the file
      val oos = new ObjectOutputStream(new FileOutputStream(path))
      oos.writeObject(obj)
      oos.close()
    } catch {
      case e: Exception => logger.error(s"Error storing AlertsDb to path - $path ", e)
    }
  }

  /**
   * Mark an alert checked, so it won't be queried again within a time period
   * @param alert The Alert
   * @param now A timestamp, visible for testing
   */
  def markChecked(alert: AlertRule, now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Unit =
    this.synchronized {
      val alertRemoved = checkedAlertRules.filterNot {
        case (a, _) =>
          a == alert
      }

      checkedAlertRules = alertRemoved :+ (alert, now)
    }

  /**
   * Test if we should check an alert rule again. If the alert is silenced (because it has alerted
   * recently) or we have recently checked this rule, the function will return <code>false</code>
   * @param alertRule the alert rule
   * @param now the time now
   * @return
   */
  def shouldCheck(alertRule: AlertRule,
                  now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Boolean = {
    val insideWaitPeriod = checkedAlertRules.exists {
      case (a, time) =>
        alertRule == a && ChronoUnit.MINUTES
          .between(time, now) <= alertRule.retryInterval
    }

    !insideWaitPeriod
  }

  /**
   * Remove all old alert records.
   * @param now the time now
   */
  def clean(now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): Unit =
    this.synchronized {
      checkedAlertRules = checkedAlertRules.filter {
        case (alert, alertedTime) =>
          ChronoUnit.MINUTES
            .between(alertedTime, now) <= alert.retryInterval
      }
    }

  /**
   * Cleans the AlertsDb and persists it in a file.
   */
  def close(): Unit = {
    clean()
    persist()
  }

  /**
   * Remove all records from the database. This should be used for testing only, calling this method
   * clears all data about when alerts were checked
   */
  def reset(): Unit =
    checkedAlertRules = List()

}
