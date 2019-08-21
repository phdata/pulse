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
import java.nio.file.Paths
import java.util.UUID

import org.apache.commons.io.FileUtils
import org.apache.solr.client.solrj.embedded.JettyConfig
import org.apache.solr.cloud.MiniSolrCloudCluster

object TestUtil {

  def miniSolrCloudCluster(): MiniSolrCloudCluster = {
    // clean up the solr files so we don't try to read collections from old runs
    FileUtils.deleteDirectory(new File("target/solr7"))

    // Set up a MiniSolrCloudCluster
    val clusterHome =
      s"${System.getProperty("user.dir")}/target/solr7/solrHome/${UUID.randomUUID()}"
    val jettyConfig =
      JettyConfig.builder().setContext("/solr").setPort(8983).stopAtShutdown(true).build()

    new MiniSolrCloudCluster(1,
                             null,
                             Paths.get(clusterHome),
                             MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML,
                             null,
                             null)
  }

  def randomIdentifier() = UUID.randomUUID().toString.substring(0, 5)
}
