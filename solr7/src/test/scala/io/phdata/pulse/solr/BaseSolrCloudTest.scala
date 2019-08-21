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

package io.phdata.pulse.solr

import java.io.File

import org.apache.solr.client.solrj.impl.{ CloudSolrClient, ZkClientClusterStateProvider }
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

trait BaseSolrCloudTest extends FunSuite with BeforeAndAfterAll {
  val TEST_CONF_NAME              = "testconf"
  val miniSolrCloudCluster        = TestUtil.miniSolrCloudCluster()
  val solrClient: CloudSolrClient = miniSolrCloudCluster.getSolrClient

  def solrService =
    SolrProvider.fromClient(List(miniSolrCloudCluster.getZkServer.getZkAddress), solrClient)

  def addDocument(document: Map[String, String]): UpdateResponse =
    solrClient.add(mapToSolrDocument(document))

  def mapToSolrDocument(event: Map[String, _]): SolrInputDocument = {
    val doc = new SolrInputDocument()
    event.foreach(field => doc.addField(field._1, field._2))
    doc
  }

  override def beforeAll(): Unit = {
    val zkProvider = new ZkClientClusterStateProvider(solrClient.getZkStateReader)

    zkProvider.uploadConfig(
      new File(System.getProperty("user.dir") + "/test-config/solr7/conf").toPath,
      TEST_CONF_NAME)
  }
}
