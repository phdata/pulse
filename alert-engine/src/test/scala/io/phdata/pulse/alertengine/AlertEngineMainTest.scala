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

}
