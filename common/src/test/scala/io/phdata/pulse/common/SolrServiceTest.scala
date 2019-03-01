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

package io.phdata.pulse.common

import java.io.File

import io.phdata.pulse.testcommon.{ BaseSolrCloudTest, TestUtil }
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.util.NamedList
import org.scalatest.FunSuite

class SolrServiceTest extends FunSuite with BaseSolrCloudTest {
  val solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

  override def beforeAll(): Unit = {
    super.beforeAll()
    solrService.uploadConfDir(
      new File(System.getProperty("user.dir") + "/test-config/solr_configs/conf").toPath,
      "testconf")
  }

  test("create collection") {
    val collectionName = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)

    assert(solrService.collectionExists(collectionName))
  }

  test("check if collection exists") {
    val collectionName = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)

    assert(solrService.collectionExists(collectionName))
  }

  test("check if alias exists") {
    val collectionName = TestUtil.randomIdentifier()
    val alias1Name     = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)
    solrService.createAlias(alias1Name, collectionName)

    assert(solrService.aliasExists(alias1Name))
  }

  test("recreating existing collection throws error") {
    val collectionName = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)

    intercept[Exception] {
      solrService.createCollection(collectionName, 1, 1, "testconf", null)
    }
  }

  test("delete collection") {
    val collectionName = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)
    solrService.deleteCollection(collectionName)

    assert(!solrService.collectionExists(collectionName))
  }

  test("create alias to single collection") {
    val collectionName = TestUtil.randomIdentifier()
    val aliasName      = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)
    solrService.createAlias(aliasName, collectionName)

    assert(solrService.aliasExists(aliasName))
  }

  test("getAlias when exists returns a Some") {
    val collectionName = TestUtil.randomIdentifier()
    val aliasName      = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)
    solrService.createAlias(aliasName, collectionName)

    assertResult(Some(Set(collectionName)))(solrService.getAlias(aliasName))
  }

  test("getAlias when it doesn't exist returns None") {
    val alias1Name = TestUtil.randomIdentifier()

    assertResult(None)(solrService.getAlias("doesNotExist"))
  }

  test("deleteAlias deletes alias") {
    val collectionName = TestUtil.randomIdentifier()
    val alias1Name     = TestUtil.randomIdentifier()

    solrService.createCollection(collectionName, 1, 1, "testconf", null)
    solrService.createAlias(alias1Name, collectionName)
    solrService.deleteAlias(alias1Name)

    assert(!solrService.aliasExists(alias1Name))
  }

  test("create alias to multiple collections") {
    val aliasName       = TestUtil.randomIdentifier()
    val collection1Name = TestUtil.randomIdentifier()
    val collection2Name = TestUtil.randomIdentifier()

    solrService.createCollection(collection1Name, 1, 1, "testconf", null)
    solrService.createCollection(collection2Name, 1, 1, "testconf", null)
    solrService.createAlias(aliasName, collection1Name, collection2Name)

    assertResult(Some(Set(collection1Name, collection2Name)))(solrService.getAlias(aliasName))
  }

  test("Documents are added to solr collection") {
    val collectionName = TestUtil.randomIdentifier()
    solrService.createCollection(collectionName, 1, 1, "testconf", null)

    val document1 = DocumentConversion.mapToSolrDocument(
      Map(
        "category"   -> "ERROR",
        "timestamp"  -> "1970-01-01T00:00:00Z",
        "level"      -> "ERROR",
        "message"    -> "message",
        "threadName" -> "thread oxb",
        "throwable"  -> "Exception in thread main"
      ))

    val document2 = DocumentConversion.mapToSolrDocument(
      Map(
        "category"   -> "ERROR",
        "timestamp"  -> "1970-01-01T00:00:00Z",
        "level"      -> "ERROR",
        "message"    -> "message",
        "threadName" -> "thread oxb",
        "throwable"  -> "Exception in thread main"
      ))

    solrService.insertDocuments(collectionName, Seq(document1, document2))

    val query = new SolrQuery
    query.setQuery(s"level: ERROR")
    query.add("collection", collectionName)
    val response = solrClient.query(query)
    val list     = response.getResults

    assert(list.getNumFound === 2)
  }

  test("Insert properties dynamic fields from mdc") {
    val collectionName = TestUtil.randomIdentifier()
    solrService.createCollection(collectionName, 1, 1, "testconf", null)

    val document1 = DocumentConversion.mapToSolrDocument(
      Map(
        "category"   -> "ERROR",
        "timestamp"  -> "1970-01-01T00:00:00Z",
        "level"      -> "ERROR",
        "message"    -> "message",
        "threadName" -> "thread oxb",
        "throwable"  -> "Exception in thread main",
        "hostname"   -> "host1.com"
      ))

    solrService.insertDocuments(collectionName, Seq(document1))

    val query = new SolrQuery
    query.setQuery(s"hostname: host1.com")
    query.add("collection", collectionName)
    val response = solrClient.query(query)
    val list     = response.getResults

    assert(list.getNumFound === 1)
  }

  private def getStatusCode(result: NamedList[AnyRef]) =
    result.get("responseHeader").asInstanceOf[NamedList[Object]].get("status")
}
