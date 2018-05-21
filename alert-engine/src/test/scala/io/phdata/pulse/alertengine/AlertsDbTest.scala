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

import java.time.ZonedDateTime

import org.scalatest.{ BeforeAndAfterEach, FunSuite }

class AlertsDbTest extends FunSuite with BeforeAndAfterEach {
  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
  }

  test("adding a duplicate alert to the db updates the sleep time") {
    val alert      = AlertRule("query", 1, Some(0), List("tony@phdata.io"))
    val firstTime  = ZonedDateTime.now()
    val secondTime = firstTime.plusMinutes(6)

    AlertsDb.markChecked(alert, secondTime)

    assertResult(false)(AlertsDb.shouldCheck(alert, secondTime))
  }

  test("notify on an unseen alert") {
    val alert = AlertRule("query", 1, Some(0), List("tony@phdata.io"))
    val now   = ZonedDateTime.now()

    assertResult(true)(AlertsDb.shouldCheck(alert, now))
  }

  test("don't notify inside alert window") {
    val alert = AlertRule("query", 1, Some(0), List("tony@phdata.io"))
    val now   = ZonedDateTime.now()

    AlertsDb.markChecked(alert, now)

    assertResult(false)(AlertsDb.shouldCheck(alert, now))
  }

  test("don't alert on an alert that was just checked") {
    val alert = AlertRule("query", 1, Some(0), List("tony@phdata.io"))
    val now   = ZonedDateTime.now()

    AlertsDb.markChecked(alert, now)

    assertResult(false)(AlertsDb.shouldCheck(alert))
  }

  test("serialization and deserialization of AlertsDb") {
    AlertsDb.load()

    val alert = AlertRule("query", 1, Some(0), List("tony@phdata.io"))
    val now   = ZonedDateTime.now()

    // check serializing and de-serializing of checkedAlertRules
    AlertsDb.markChecked(alert, now)

    AlertsDb.persist()
    AlertsDb.reset()
    AlertsDb.load()

    assertResult(false)(AlertsDb.shouldCheck(alert, now))

    AlertsDb.persist()
    AlertsDb.reset()
    AlertsDb.load()

    assertResult(false)(AlertsDb.shouldCheck(alert, now))
    assertResult(false)(AlertsDb.shouldCheck(alert, now))

    AlertsDb.reset()
    AlertsDb.persist()
  }
}
