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

package io.phdata.pulse.testcommon

import java.io.File

import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.apache.solr.cloud.MiniSolrCloudCluster
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

trait BaseSolrCloudTest extends FunSuite with BeforeAndAfterAll {
  val TEST_CONF_NAME                             = "testconf"
  val miniSolrCloudCluster: MiniSolrCloudCluster = TestUtil.miniSolrCloudCluster()
  def solrClient(): CloudSolrServer              = miniSolrCloudCluster.getSolrClient

  override def beforeAll(): Unit =
    miniSolrCloudCluster.getSolrClient.uploadConfig(
      new File(System.getProperty("user.dir") + "/test-config/solr_configs/conf").toPath,
      TEST_CONF_NAME)
}
