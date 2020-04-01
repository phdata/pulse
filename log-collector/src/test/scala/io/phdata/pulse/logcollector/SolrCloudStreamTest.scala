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

import io.phdata.pulse.common.domain.LogEvent
import io.phdata.pulse.solr.{ BaseSolrCloudTest, TestUtil }
import org.apache.solr.client.solrj.SolrQuery
import org.scalatest.FunSuite

class SolrCloudStreamTest extends FunSuite with BaseSolrCloudTest {
  val TEST_COLLECTION = TestUtil.randomIdentifier()

  val streamProcessor =
    new SolrCloudStream(solrService)

  val document1 = Util.logEventToFlattenedMap(
    new LogEvent(None,
                 "ERROR",
                 "1970-01-01T00:00:00Z",
                 "ERROR",
                 "message 1",
                 "thread oxb",
                 Some("Exception in thread main"),
                 None))

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

    val testValues = List.fill(StreamParams().batchSize + 100)((app1Name, document1))

    testValues.foreach(x => streamProcessor.put(x._1, x._2))

    // sleep past the time documents would be flushed

    Thread.sleep(3000)

    val app1Query = new SolrQuery("level: ERROR")
    app1Query.set("collection", app1Alias)

    val queryResult = solrClient.query(app1Query)

    assertResult(1100)(queryResult.getResults.getNumFound)
  }

  test("Test flow groups by application id, flushes after max documents") {
    val app1Name = TestUtil.randomIdentifier()

    val collection = s"${app1Name}_1"
    val alias      = s"${app1Name}_latest"

    solrService.createCollection(collection, 1, 1, TEST_CONF_NAME, null)
    solrService.createAlias(alias, collection)

    val testValues = List.fill(StreamParams().batchSize + 100)((app1Name, document1))

    testValues.foreach(x => streamProcessor.put(x._1, x._2))

    Thread.sleep(3000)

    val query = new SolrQuery("level: ERROR")
    query.set("collection", alias)

    val queryResult = solrClient.query(query)

    assert(queryResult.getResults.getNumFound > 0)
  }

  ignore("Stream failure method called if it exceeds the maximum queue size") {
    // Variable to signal the stream failure method has been called
    var streamFailed = false

    // scalastyle:off null
    val stream = new SolrCloudStream(null) {
      // Assert that this function is called without calling `System.exit` like the original function does
      override private[logcollector] def exitingOverflowMessage[A](numEvents: Long): Option[A] = {
        streamFailed = true
        None
      }

      // Override the method and sleep with the intention of backing up the stream
      override private[logcollector] def save(appName: String, events: Seq[Map[String, String]]) =
        Thread.sleep(100)
    }
    assert(!streamFailed)

    // put a lot of messages on the stream
    (1 to 1000000) foreach { x =>
      stream.put("app1", Map("foo" -> "bar"))
    }

    Thread.sleep(3000)

    assert(streamFailed)
  }

}
