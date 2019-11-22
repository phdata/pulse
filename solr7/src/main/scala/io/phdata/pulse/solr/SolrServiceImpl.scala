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

import java.nio.file.Path
import java.util

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.{BinaryResponseParser, CloudSolrClient}
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION
import org.apache.solr.client.solrj.request.{QueryRequest, UpdateRequest}
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.cloud.{SolrZkClient, ZkConfigManager, ZkStateReader}
import org.apache.solr.common.params.CollectionParams.CollectionAction
import org.apache.solr.common.params.{CollectionAdminParams, CoreAdminParams, ModifiableSolrParams}
import org.apache.solr.common.util.NamedList

import scala.collection.JavaConversions._

class SolrServiceImpl(zkAddresses: List[String], solr: CloudSolrClient) extends SolrService {
  private val ZK_CLIENT_TIMEOUT         = 30000
  private val ZK_CLIENT_CONNECT_TIMEOUT = 30000

  private val zkClient =
    new SolrZkClient(zkAddresses.head, ZK_CLIENT_TIMEOUT, ZK_CLIENT_CONNECT_TIMEOUT, null)

  private val manager = new ZkConfigManager(zkClient)

  override def uploadConfDir(path: Path, name: String): Unit = manager.uploadConfigDir(path, name)

  override def createCollection(name: String,
                                numShards: Int,
                                replicationFactor: Int,
                                configName: String,
                                createNodeSet: String,
                                collectionProperties: util.Map[String, String] =
                                  new util.HashMap[String, String]()): Unit = {
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

  override def collectionExists(name: String): Boolean =
    listCollections().contains(name)

  override def listCollections(): List[String] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.LIST.name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    val result = makeRequest(request)

    result
      .asInstanceOf[NamedList[Object]]
      .get("collections")
      .asInstanceOf[java.util.List[String]]
      .toList
  }

  override def getAlias(name: String): Option[Set[String]] = listAliases().get(name)

  override def aliasExists(name: String): Boolean = {
    logger.info(s"searching for alias existence: $name")
    listAliases().exists(_._1 == name)
  }

  override def listAliases(): Map[String, Set[String]] = {
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

  def clusterStatus(): NamedList[Any] = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.CLUSTERSTATUS.name)

    val request = new QueryRequest(params)
    request.setResponseParser(new BinaryResponseParser())
    request.setPath("/admin/collections")

    makeRequest(request)
  }

  override def query(collection: String, query: String): Seq[Map[String, _]] = {
    logger.info("solrService is starting querying")
    val solrQuery = new SolrQuery(query)
    logger.info("solrService is created solrQuery")
    solrQuery.set("fl", "*") // return full results
    solrQuery.set("collection", collection)
    val results = solr.query(solrQuery).getResults
    logger.info(s"solrQuery results are $results")
    results.toSeq.map(_.entrySet().toSeq.map(x => x.getKey -> x.getValue).toMap)
  }

  def makeRequest(request: QueryRequest): NamedList[Any] = {
    logger.debug(s"Solr request: $request")
    val result = solr.request(request)
    logger.debug("solr result" + result.toString)
    val failure = result.get("failure")
    if (failure != null) {
      throw new Exception(result.toString)
    }
    result.asInstanceOf[NamedList[Any]]
  }

  override def createAlias(name: String, collections: String*): Unit = {
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

  /**
   * Convert a [[Map[String, String]]] to a [[SolrInputDocument]]
   * @param event [[Map[String, String]]] to convert
   * @return The document as a [[SolrDocument]]
   */
  private def mapToSolrDocument(event: Map[String, _]): SolrInputDocument = {
    val doc = new SolrInputDocument()
    event.foreach(field => doc.addField(field._1, field._2))
    doc
  }

  override def insertDocuments(collection: String, documents: Seq[Map[String, _]]): Unit = {

    logger.info(s"saving ${documents.length} documents to collection '$collection'")
    logger.trace(s"saving documents " + documents.mkString("\n"))

    val update_request = new UpdateRequest()
    update_request.setParam("collection", collection)
    documents.foreach { doc =>
      update_request.add(mapToSolrDocument(doc))
    }

    update_request.setAction(ACTION.COMMIT, true, true, true).process(solr)

    logger.info(s"saved ${documents.length} documents to collection $collection")
  }

  override def deleteCollection(name: String): Unit = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.DELETE.name)
    params.set(CoreAdminParams.NAME, name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    logger.info(s"Deleting collection: $name")

    makeRequest(request)
  }

  override def deleteAlias(name: String): Unit = {
    val params = new ModifiableSolrParams
    params.set(CoreAdminParams.ACTION, CollectionAction.DELETEALIAS.name)
    params.set(CoreAdminParams.NAME, name)

    val request = new QueryRequest(params)
    request.setPath("/admin/collections")

    makeRequest(request)
  }

  override def close(): Unit = {
    try {
      solr.close()
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
