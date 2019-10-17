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

import io.phdata.pulse.alertengine.{ AlertsDb, TestObjectGenerator }
import io.phdata.pulse.solr.{ BaseSolrCloudTest, TestUtil }
import org.scalatest.BeforeAndAfterEach

class SolrAlertTriggerTest extends BaseSolrCloudTest with BeforeAndAfterEach {
  val CONF_NAME               = "testconf"
  private val applicationName = TestUtil.randomIdentifier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    AlertsDb.reset()
    val alias = applicationName + "_all"

    solrClient.setDefaultCollection(alias)
    solrService.createCollection(alias, 1, 1, CONF_NAME, null)

    Seq[Map[String, String]](
      Map("id" -> "1", "level" -> "ERROR", "message" -> "sad"),
      Map("id" -> "2", "level" -> "INFO", "message"  -> "happy"),
      Map("id" -> "3", "level" -> "ERROR", "message" -> "very sad")
    ).foreach(addDocument)

    solrClient.commit(true, true, true)
    // unset the default collection so we are sure it is being set in the request
    solrClient.setDefaultCollection("")
  }

  private def stripVersion(result: Seq[Map[String, Any]]): Seq[Map[String, Any]] = {
    result.collect {
      case doc: Map[String, Any] =>
      assert(doc.contains("_version_"))
      doc - "_version_"
    }
  }

  test("query returns matching documents") {
    val alertRule =
      TestObjectGenerator.alertRule(
        query = "level:ERROR",
        retryInterval = 1,
        resultThreshold = Some(1),
        alertProfiles = List("test@phdata.io")
      )
    val expectedDocuments = Seq(
      Map("id" -> "1", "level" -> "ERROR", "message" -> "sad"),
      Map("id" -> "3", "level" -> "ERROR", "message" -> "very sad")
    )

    val trigger = new SolrAlertTrigger(solrService)
    val result  = trigger.query(applicationName, alertRule)
    val modifiedResult = stripVersion(result)
    assertResult(expectedDocuments)(modifiedResult)
  }

  test("query returns no documents") {
    val alertRule = TestObjectGenerator.alertRule(query = "level:WARN")

    val trigger = new SolrAlertTrigger(solrService)
    assertResult(Seq.empty)(trigger.query(applicationName, alertRule))
  }

  test("invalid query") {
    val alertRule = TestObjectGenerator.alertRule(query = ":")

    val trigger = new SolrAlertTrigger(solrService)
    assertThrows[Exception](trigger.query(applicationName, alertRule))
  }

}
