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

import java.util.Optional

import io.phdata.pulse.common.{ SolrService, SolrServiceImpl }
import org.apache.solr.client.solrj.impl.CloudSolrClient

import scala.collection.JavaConverters._

object SolrProvider {

  def create(zkAddresses: List[String]): SolrService = {
    val solrClient = client((zkAddresses))
    new SolrServiceImpl(zkAddresses, solrClient)
  }

  private def client(zkAddresses: List[String]) =
    new CloudSolrClient.Builder(zkAddresses.asJava, Optional.of("/solr")).build()

  def fromClient(zkAddresses: List[String], solr: CloudSolrClient) =
    new SolrServiceImpl(zkAddresses, solr)
}
