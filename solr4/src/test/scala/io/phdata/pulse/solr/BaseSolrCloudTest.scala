/*
 * Copyright 2019 phData Inc.
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

import io.phdata.pulse.common.SolrServiceImpl
import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.cloud.MiniSolrCloudCluster
import org.apache.solr.common.SolrInputDocument
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

trait BaseSolrCloudTest extends FunSuite with BeforeAndAfterAll {
  val TEST_CONF_NAME                             = "testconf"
  val miniSolrCloudCluster: MiniSolrCloudCluster = TestUtil.miniSolrCloudCluster()
  val solrClient: CloudSolrServer                = miniSolrCloudCluster.getSolrClient

  override def beforeAll(): Unit =
    miniSolrCloudCluster.getSolrClient.uploadConfig(
      new File(System.getProperty("user.dir") + "/test-config/solr4/conf").toPath,
      TEST_CONF_NAME)

  def addDocument(document: Map[String, String]): UpdateResponse =
    solrClient.add(mapToSolrDocument(document))

  def solrService: SolrServiceImpl =
    SolrProvider.fromClient(List(miniSolrCloudCluster.getZkServer.getZkAddress), solrClient)

  def mapToSolrDocument(event: Map[String, _]): SolrInputDocument = {
    val doc = new SolrInputDocument()
    event.foreach(field => doc.addField(field._1, field._2))
    doc
  }
}
