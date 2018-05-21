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

class AlertEngineCliParserTest extends FunSuite {

  test("Test AlertEngineCliParse") {

    val args = Array(
      "--conf",
      "sample conf",
      "--smtp-server",
      "sample.smtpserver.com",
      "--smtp-user",
      "raymondblanc",
      "--smtp-password",
      "raymondblanc_password",
      "--smtp-port",
      "49152",
      "--silenced-application-file",
      "silenced-applications.txt",
      "--zk-hosts",
      "master1.valhalla.phdata.io/solr,master2.valhalla.phdata.io/solr,master3.valhalla.phdata.io/solr"
    )

    val cliParser = new AlertEngineCliParser(args)

    assertResult("sample.smtpserver.com")(cliParser.smtpServer())
    assertResult("sample conf")(cliParser.conf())
    assertResult("raymondblanc")(cliParser.smtpUser())
    assertResult("raymondblanc_password")(cliParser.smtpPassword())
    assertResult(49152)(cliParser.smtpPort())
    assertResult("silenced-applications.txt")(cliParser.silencedApplicationsFile())
    assertResult(
      "master1.valhalla.phdata.io/solr,master2.valhalla.phdata.io/solr,master3.valhalla.phdata.io/solr")(
      cliParser.zkHosts())

  }

}
