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

import java.io.Closeable
import java.nio.file.Path
import java.util

import com.typesafe.scalalogging.LazyLogging
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.impl.{ BinaryResponseParser, CloudSolrServer }
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION
import org.apache.solr.client.solrj.request.{ QueryRequest, UpdateRequest }
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.cloud.{ SolrZkClient, ZkConfigManager, ZkStateReader }
import org.apache.solr.common.params.CollectionParams.CollectionAction
import org.apache.solr.common.params.{
  CollectionAdminParams,
  CoreAdminParams,
  ModifiableSolrParams
}
import org.apache.solr.common.util.NamedList

import scala.collection.JavaConversions._

class SolrService(zkAddress: String, solr: CloudSolrServer) extends Closeable with LazyLogging {
  private val ZK_CLIENT_TIMEOUT         = 30000
  private val ZK_CLIENT_CONNECT_TIMEOUT = 30000

  private val zkClient =
    new SolrZkClient(zkAddress, ZK_CLIENT_TIMEOUT, ZK_CLIENT_CONNECT_TIMEOUT, null)

  private val manager = new ZkConfigManager(zkClient)

  def uploadConfDir(path: Path, name: String): Unit = manager.uploadConfigDir(path, name)

  def createCollection(name: String,
                       numShards: Int,
                       replicationFactor: Int,
                       configName: String,
                       createNodeSet: String,
                       collectionProperties: util.Map[String, String] =
                         new util.HashMap[String, String]()): NamedList[AnyRef] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.CREATE.name)
    params.set(CoreAdminParams.NAME, name)
    params.set(ZkStateReader.NUM_SHARDS_PROP, numShards)
    params.set("replicationFactor", replicationFactor)
    params.set("collection.configName", configName)

    if (null != createNodeSet) {
      params.set(CollectionAdminParams.CREATE_NODE_SET_PARAM, createNodeSet)
    }
    if (collectionProperties != null) {
      for (property <- collectionProperties.entrySet) {
        params.set(CoreAdminParams.PROPERTY_PREFIX + property.getKey, property.getValue)
      }
    }

    val request = new QueryRequest(params)
    logger.info(s"Creating collection: $name")

    request.setPath("/admin/collections")
    makeRequest(request)
  }

  def collectionExists(name: String): Boolean =
    listCollections().contains(name)

  def listCollections(): List[String] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.LIST.name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    val result = makeRequest(request)

    result
      .asInstanceOf[NamedList[AnyRef]]
      .get("collections")
      .asInstanceOf[java.util.List[String]]
      .toList
  }

  def getAlias(name: String): Option[Set[String]] = listAliases().get(name)

  def aliasExists(name: String): Boolean = {
    logger.info(s"searching for alias existence: $name")
    listAliases().exists(_._1 == name)
  }

  def listAliases(): Map[String, Set[String]] = {
    val aliases: Any = clusterStatus()
      .get("cluster")
      .asInstanceOf[org.apache.solr.common.util.SimpleOrderedMap[String]]
      .get("aliases")

    if (aliases == null) {
      logger.info(s"no alias block found")
      Map()
    } else {
      logger.debug(s"found aliases: $aliases")

      aliases
        .asInstanceOf[java.util.LinkedHashMap[String, String]]
        .map {
          case (aliasName, collections) =>
            (aliasName, collections.split(',').toSet)
        }
        .toMap
    }
  }

  def clusterStatus(): NamedList[AnyRef] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.CLUSTERSTATUS.name)

    val request = new QueryRequest(params)
    request.setResponseParser(new BinaryResponseParser())
    request.setPath("/admin/collections")

    makeRequest(request)
  }

  def makeRequest(request: SolrRequest): NamedList[AnyRef] = {
    logger.debug(s"Solr request: $request")
    val result: NamedList[AnyRef] = solr.request(request)
    logger.debug("solr result" + result.toString)
    val failure = result.get("failure")
    if (failure != null) {
      throw new Exception(result.toString)
    }
    result
  }

  def createAlias(name: String, collections: String*): NamedList[AnyRef] = {
    val collectionsSeq = collections.mkString(",")
    val params         = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.CREATEALIAS.name)
    params.set(CoreAdminParams.NAME, name)
    params.set("collections", collectionsSeq)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    logger.info(s"creating alias $name pointing at $collectionsSeq")
    makeRequest(request)
  }

  def insertDocuments(collection: String, documents: Seq[SolrInputDocument]): Unit = {

    logger.info(s"saving ${documents.length} documents to collection '$collection'")
    logger.trace(s"saving documents " + documents.mkString("\n"))

    val update_request = new UpdateRequest()
    update_request.setParam("collection", collection)
    documents.foreach { doc =>
      update_request.add(doc)
    }

    println(update_request.getXML)

    update_request.setAction(ACTION.COMMIT, true, true, true).process(solr)

    logger.info(s"saved ${documents.length} documents to collection $collection")
  }

  def deleteCollection(name: String): NamedList[AnyRef] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.DELETE.name)
    params.set(CoreAdminParams.NAME, name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    logger.info(s"Deleting collection: $name")

    makeRequest(request)
  }

  def deleteAlias(name: String): NamedList[AnyRef] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.DELETEALIAS.name)
    params.set(CoreAdminParams.NAME, name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    makeRequest(request)
  }

  def close(): Unit = {
    try {
      solr.shutdown()
    } catch {
      case e: Exception => logger.warn("Couldn't close solr client cleanly", e)
    }

    try {
      zkClient.close()
    } catch {
      case e: Exception => logger.warn("Couldn't close zkClient cleanly", e)
    }
  }
}
