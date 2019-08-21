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

package io.phdata.pulse.common

import java.nio.file.Path
import java.util

import com.typesafe.scalalogging.LazyLogging

trait SolrService extends LazyLogging {

  def uploadConfDir(path: Path, name: String): Unit

  def createCollection(name: String,
                       numShards: Int,
                       replicationFactor: Int,
                       configName: String,
                       createNodeSet: String,
                       collectionProperties: util.Map[String, String] =
                         new util.HashMap[String, String]()): Unit

  def collectionExists(name: String): Boolean =
    listCollections().contains(name)

  def listCollections(): List[String]

  def getAlias(name: String): Option[Set[String]]

  def aliasExists(name: String): Boolean

  def listAliases(): Map[String, Set[String]]

  def createAlias(name: String, collections: String*): Unit

  def insertDocuments(collection: String, documents: Seq[Map[String, _]]): Unit

  def query(collection: String, query: String): Seq[Map[String, _]]

  def deleteCollection(name: String): Unit

  def deleteAlias(name: String): Unit

  def close(): Unit

}
