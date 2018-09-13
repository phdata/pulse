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

import org.scalatest.FunSuite

class AlertEngineConfigParserTest extends FunSuite {

  test("parse config") {
    val yaml =
      """---
        |applications:
        |- name: application1
        |  alertRules:
        |  - query: "query"
        |    retryInterval: 10
        |    resultThreshold: 0
        |    alertProfiles:
        |    - mailProfile1
        |    - slackProfile1
        |  emailProfiles:
        |  - name: mailProfile1
        |    addresses:
        |    - tony@phdata.io
        |  slackProfiles:
        |  - name: slackProfile1
        |    url : testurl.com
        |  """.stripMargin

    val expected =
      AlertEngineConfig(
        List(
          Application(
            "application1",
            List(AlertRule("query", 10, Some(0), List("mailProfile1", "slackProfile1"))),
            Some(List(TestObjectGenerator.mailAlertProfileTestObject(name = "mailProfile1", addresses = List("tony@phdata.io")))),
            Some(List(TestObjectGenerator.slackAlertProfile()))
          ))
      )
    println(yaml)
    assertResult(expected)(AlertEngineConfigParser.convert(yaml))
  }
}
