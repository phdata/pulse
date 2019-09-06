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

  /*
   *function to set environmental variables for testing
   */
  private def setEnv(key: String, value: String): String = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map =
      field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

  private def unsetEnv(key: String): String = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map =
      field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.remove(key)
  }

  test("Test Env variable setup") {
    setEnv("SMTP_PASSWORD", "pa$$word")
    setEnv("SMTP_USER", "testing@company.com")

    val user: Option[String]                    = sys.env.get("SMTP_USER")
    val password: Option[String]                = sys.env.get("SMTP_PASSWORD")
    val noneExistingEnvVariable: Option[String] = sys.env.get("xxx_xx_xxx_non_evn_variable")

    assertResult(Some("testing@company.com"))(user)
    assertResult(Some("pa$$word"))(password)
    assertResult(None)(noneExistingEnvVariable)

    unsetEnv("SMTP_PASSWORD")
    unsetEnv("SMTP_USER")
  }

  test("Test AlertEngineCliParse") {
    val args = Array(
      "--conf",
      "sample conf",
      "--smtp-server",
      "sample.smtpserver.com",
      "--smtp-user",
      "testing@company.com",
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
    assertResult("testing@company.com")(cliParser.smtpUser())
    assertResult(49152)(cliParser.smtpPort())
    assertResult("silenced-applications.txt")(cliParser.silencedApplicationsFile())
    assertResult(
      List("master1.valhalla.phdata.io/solr",
           "master2.valhalla.phdata.io/solr",
           "master3.valhalla.phdata.io/solr").mkString(","))(cliParser.zkHost())
    assertResult(None)(cliParser.smtpPassword)
  }

  test("Test database url") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-url=jdbc/something"
    )

    val cliParser = new AlertEngineCliParser(args)

    assertResult("jdbc/something")(cliParser.dbUrl())
  }

  test("Test database user") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-user=me"
    )

    val cliParser = new AlertEngineCliParser(args)
    assertResult("me")(cliParser.dbUser())
  }

  test("Test database password") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-password=hello"
    )

    val cliParser = new AlertEngineCliParser(args)
    assertResult("hello")(cliParser.dbPassword())
  }

  test("Test database options") {
    val args = Array(
      "--conf",
      "sample conf",
      "--db-options=key1=value1;key2=value2"
    )

    val cliParser = new AlertEngineCliParser(args)
    assertResult("key1=value1;key2=value2")(cliParser.dbOptions())
  }

}
