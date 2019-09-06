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

import java.io.PrintWriter

import org.scalatest.FunSuite

class AlertEngineMainTest extends FunSuite {

  val silencedApplications = List("app1", "app2", "app3")
  val silencedAlertsPath   = "target/silenced-alerts.txt"
  val writer               = new PrintWriter(silencedAlertsPath)
  silencedApplications.foreach(app => writer.println(app))
  writer.flush()

  test("read silenced applications") {
    assert(
      AlertEngineMain
        .readSilencedApplications(silencedAlertsPath)
        .toSet == silencedApplications.toSet)
  }

  test("don't fail if silenced application file isn't there") {
    AlertEngineMain
      .readSilencedApplications("/foo/bar")
  }

  test("findAlertTypes finds the expected types") {
    val alertRule1 = TestObjectGenerator.alertRule(query = "spark2query1", alertType = Some(AlertTypes.SQL))
    val alertRule2 = TestObjectGenerator.alertRule(query = "spark2query2", alertType=Some(AlertTypes.SQL))
    val alertRule3 = TestObjectGenerator.alertRule(query = "spark2query3", alertType=Some(AlertTypes.SOLR))
    val apps = List(
      Application("spark1", List(alertRule1), None, None),
      Application("spark2", List(alertRule2, alertRule3), None, None)
    )

    val results = AlertEngineMain.findAlertTypes(apps)
    assertResult(Set(AlertTypes.SOLR, AlertTypes.SQL))(results)
  }

  test("findAlertTypes throws exception on unknown alert type") {
    val alertRule1 = TestObjectGenerator.alertRule(query = "spark2query1", alertType = Some(AlertTypes.SQL))
    val alertRule2 = TestObjectGenerator.alertRule(query = "spark2query2", alertType=Some("blah"))
    val alertRule3 = TestObjectGenerator.alertRule(query = "spark2query3", alertType=Some(AlertTypes.SOLR))
    val apps = List(
     Application("spark1", List(alertRule1), None, None),
     Application("spark2", List(alertRule2, alertRule3), None, None)
    )

    assertThrows[IllegalStateException](AlertEngineMain.findAlertTypes(apps))
  }

  test("findAlertTypes returns solr alert type when alert type is not defined") {
    val alertRule1 = TestObjectGenerator.alertRule(query = "spark2query1", alertType = None)
    val apps = List(
      Application("spark1", List(alertRule1), None, None)
    )

    assertResult(Set(AlertTypes.SOLR))(AlertEngineMain.findAlertTypes(apps))
  }

  test("createSolrAlertTrigger fails without zkHost") {
    val args = Array(
      "--conf",
      "sample conf"
    )

    val parser = new AlertEngineCliParser(args)
    assertThrows[IllegalStateException](AlertEngineMain.createSolrAlertTrigger(parser))
  }

  test("createSqlAlertTrigger fails without database URL") {
    val args = Array(
      "--conf",
      "sample conf"
    )

    val parser = new AlertEngineCliParser(args)
    assertThrows[IllegalStateException](AlertEngineMain.createSqlAlertTrigger(parser))
  }

  test("createSqlAlertTrigger fails on invalid db options") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-url=hello",
      "--db-options=blarg=1=2"
    )

    val parser = new AlertEngineCliParser(args)
    assertThrows[IllegalArgumentException](AlertEngineMain.createSqlAlertTrigger(parser))
  }

  test("createSqlAlertTrigger user is None") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-url=hello",
      "--db-user="
    )

    val parser = new AlertEngineCliParser(args)
    val trigger = AlertEngineMain.createSqlAlertTrigger(parser)
    assertResult(None)(trigger.dbUser)
  }

  test("createSqlAlertTrigger password is None") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-url=hello",
      "--db-password="
    )

    val parser = new AlertEngineCliParser(args)
    val trigger = AlertEngineMain.createSqlAlertTrigger(parser)
    assertResult(None)(trigger.dbPassword)
  }

  test("createSqlAlertTrigger db options are empty") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-url=hello",
      "--db-options="
    )

    val parser = new AlertEngineCliParser(args)
    val trigger = AlertEngineMain.createSqlAlertTrigger(parser)
    assertResult(Map.empty)(trigger.dbOptions)
  }

}
