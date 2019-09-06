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

package io.phdata.pulse.alertengine.trigger

import io.phdata.pulse.alertengine.{AlertsDb, TestObjectGenerator}
import io.phdata.pulse.solr.TestUtil
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatestplus.mockito.MockitoSugar

class AbstractAlertTriggerTest extends FunSuite with MockitoSugar with BeforeAndAfterEach {
  val applicationName = TestUtil.randomIdentifier()

  val trigger = Mockito.mock(classOf[AbstractAlertTrigger], Mockito.CALLS_REAL_METHODS)

  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
    Mockito.reset(trigger)
  }

  test("get active alert") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
                                                  retryInterval = 1,
                                                  resultThreshold = Some(1),
                                                  alertProfiles = List("test@phdata.io"))
    when(trigger.query(applicationName, alertRule))
      .thenReturn(Seq[Map[String, Any]](Map.empty, Map.empty))

    val result = trigger.check(applicationName, alertRule).get
    assertResult(alertRule)(result.rule)
    assertResult(2)(result.documents.length)
    assertResult(applicationName)(result.applicationName)
    assertResult(2)(result.totalNumFound)
  }

  test("trigger alert when threshold is set to '-1' and there are no results") {
    val alertRule = TestObjectGenerator.alertRule(resultThreshold = Some(-1))
    when(trigger.query(applicationName, alertRule)).thenReturn(Seq.empty)

    val result = trigger.check(applicationName, alertRule).get
    assertResult(alertRule)(result.rule)
    assert(result.documents.isEmpty)
    assert(result.applicationName == applicationName)
  }

  test("don't match non alert") {
    val alertRule = TestObjectGenerator.alertRule()
    when(trigger.query(applicationName, alertRule)).thenReturn(Seq.empty)

    assertResult(None)(trigger.check(applicationName, alertRule))
  }

  test("mark alert triggered on results found > 0") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR",
                                                  retryInterval = 1,
                                                  alertProfiles = List("testing@tester.com"))
    when(trigger.query(applicationName, alertRule))
      .thenReturn(Seq[Map[String, Any]](Map.empty, Map.empty))

    assert(trigger.check(applicationName, alertRule).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(applicationName, alertRule))
  }

  test("mark alert triggered when results are found") {
    val alertRule = TestObjectGenerator.alertRule(query = "category: ERROR", retryInterval = 1)
    when(trigger.query(applicationName, alertRule))
      .thenReturn(Seq[Map[String, Any]](Map.empty, Map.empty))

    assert(trigger.check(applicationName, alertRule).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(applicationName, alertRule))
  }

  test("mark alert triggered when no results are found") {
    val alertRule = TestObjectGenerator.alertRule(resultThreshold = Some(-1))
    when(trigger.query(applicationName, alertRule)).thenReturn(Seq.empty)

    assert(trigger.check(applicationName, alertRule).isDefined)
    assertResult(false)(AlertsDb.shouldCheck(applicationName, alertRule))
  }

  test("don't mark alert triggered when no results are found") {
    val alertRule = TestObjectGenerator.alertRule(resultThreshold = Some(1))
    when(trigger.query(applicationName, alertRule)).thenReturn(Seq.empty)

    assertResult(None)(trigger.check(applicationName, alertRule))
    assertResult(true)(AlertsDb.shouldCheck(applicationName, alertRule))
  }

  test("don't check if already triggered") {
    val alertRule = TestObjectGenerator.alertRule(resultThreshold = Some(1))
    AlertsDb.markTriggered(applicationName, alertRule)
    assertResult(None)(trigger.check(applicationName, alertRule))
    assertResult(false)(AlertsDb.shouldCheck(applicationName, alertRule))
  }

  test("query exception") {
    val alertRule = TestObjectGenerator.alertRule()
    when(trigger.query(applicationName, alertRule)).thenThrow(new RuntimeException("fake bad thing"))

    assertResult(None)(trigger.check(applicationName, alertRule))
  }

}
