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

  ignore("Test flow groups by application id, flushes after GROUP_MAX_TIME") {
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
    val solrStream = streamProcessor.groupedInsert.run()

    testValues.foreach(x => solrStream ! x)

    // sleep past the time documents would be flushed
    Thread.sleep(streamProcessor.DEFAULT_BATCH_FLUSH_DURATION + 1)

    val app1Query = new SolrQuery("level: ERROR")
    app1Query.set("collection", app1Alias)

    val query1Result = solrClient.query(app1Query)

    val app2Query = new SolrQuery("level: ERROR")
    app2Query.set("collection", app2Alias)

    val query2Result = solrClient.query(app2Query)

    assertResult(2)(query1Result.getResults.getNumFound)
    assertResult(1)(query2Result.getResults.getNumFound)
  }

  test("Test flow groups by application id, flushes after max documents") {
    val app1Name = TestUtil.randomIdentifier()

    val collection = s"${app1Name}_1"
    val alias      = s"${app1Name}_latest"

    solrService.createCollection(collection, 1, 1, TEST_CONF_NAME, null)
    solrService.createAlias(alias, collection)

    val testValues = List.fill(streamProcessor.DEFALUT_GROUP_SIZE + 100)((app1Name, document1))

    val solrStream = streamProcessor.groupedInsert.run()

    testValues.foreach(x => solrStream ! x)

    val query = new SolrQuery("level: ERROR")
    query.set("collection", alias)
    // sleep until documents are flushed
    Thread.sleep(9000)

    val queryResult = solrClient.query(query)

    assert(queryResult.getResults.getNumFound > 0)
  }

}
