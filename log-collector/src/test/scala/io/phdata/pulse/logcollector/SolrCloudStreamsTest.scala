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

package io.phdata.pulse.logcollector

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.phdata.pulse.common.SolrService
import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.testcommon.{ BaseSolrCloudTest, TestUtil }
import org.apache.solr.client.solrj.SolrQuery
import org.scalatest.FunSuite

class SolrCloudStreamsTest extends FunSuite with BaseSolrCloudTest {
  val TEST_COLLECTION = TestUtil.randomIdentifier()

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val actorMaterializer        = ActorMaterializer.create(actorSystem)

  val solrService     = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)
  val streamProcessor = new SolrCloudStreams(solrService)

  val document1 = new LogEvent(None,
                               "ERROR",
                               "1970-01-01T00:00:00Z",
                               "ERROR",
                               "message 1",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  val document2 = new LogEvent(None,
                               "ERROR",
                               "1971-01-01T01:00:00Z",
                               "ERROR",
                               "message 2",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  val document3 = new LogEvent(None,
                               "ERROR",
                               "1972-01-01T02:00:00Z",
                               "ERROR",
                               "message 3",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  test("Test flow groups by application id, flushes after GROUP_MAX_TIME") {

    val app1Name = TestUtil.randomIdentifier()
    val app2Name = TestUtil.randomIdentifier()

    val app1Collection = s"${app1Name}_1"
    val app2Collection = s"${app2Name}_1"
    val app1Alias      = s"${app1Name}_latest"
    val app2Alias      = s"${app2Name}_latest"

    solrService.createCollection(app1Collection, 1, 1, TEST_CONF_NAME, null)
    solrService.createCollection(app2Collection, 1, 1, TEST_CONF_NAME, null)
    solrService.createAlias(app1Alias, app1Collection)
    solrService.createAlias(app2Alias, app2Collection)

    val testValues = Iterator((app1Name, document1), (app1Name, document2), (app2Name, document3))

    testValues.foreach(x => streamProcessor.groupedInsert.run() ! x)

    val app1Query = new SolrQuery("level: ERROR")
    app1Query.add("collection", app1Alias)

    // sleep a little bit past the time documents would be flushed
    Thread.sleep(streamProcessor.GROUP_MAX_TIME.toMillis + 500)

    val query1Result = solrClient.query(app1Query)

    assertResult(2)(query1Result.getResults.getNumFound)

    val app2Query = new SolrQuery("level: ERROR")
    app2Query.add("collection", app2Alias)

    val query2Result = solrClient.query(app2Query)

    assertResult(1)(query2Result.getResults.getNumFound)
  }

  test("Test flow groups by application id, flushes after max documents") {
    val app1Name = TestUtil.randomIdentifier()

    val app1Collection = s"${app1Name}_1"
    val app1Alias      = s"${app1Name}_latest"

    solrService.createCollection(app1Collection, 1, 1, TEST_CONF_NAME, null)
    solrService.createAlias(app1Alias, app1Collection)

    val testValues = List.fill(streamProcessor.GROUP_SIZE + 100)((app1Name, document1))

    testValues.foreach(x => streamProcessor.groupedInsert.run() ! x)

    val query = new SolrQuery("level: ERROR")
    query.add("collection", app1Alias)
    Thread.sleep(1000)

    solrClient.setDefaultCollection(app1Collection)

    val queryResult = solrClient.query(query)

    assert(queryResult.getResults.getNumFound > 0)
  }

}
