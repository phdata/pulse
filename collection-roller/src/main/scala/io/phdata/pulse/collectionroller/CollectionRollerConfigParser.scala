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

package io.phdata.pulse.collectionroller

import net.jcazevedo.moultingyaml._

import scala.io.Source

object YamlProtocol extends DefaultYamlProtocol {
  implicit val application = yamlFormat6(Application)
  implicit val config      = yamlFormat2(CollectionRollerConfig)
}

/**
 * Functionality to parse a yaml configuration into a case class
 */
object ConfigParser {
  def getConfig(path: String): CollectionRollerConfig = {
    val yamlString = Source.fromFile(path).getLines.mkString("\n")
    convert(yamlString)
  }

  def convert(yaml: String): CollectionRollerConfig = {
    import YamlProtocol._
    yaml.parseYaml.convertTo[CollectionRollerConfig]
  }
}

/**
 *
 * @param solrConfigSetDir Local directory containg one or many Solr Config Sets to be uploaded
 * @param applications List of [[Application]]s
 */
case class CollectionRollerConfig(solrConfigSetDir: String, applications: List[Application])

/**
 *
 * @param name Name of the application
 * @param numCollections Number of Solr collections to keep for the application
 * @param shards Number of Solr shards for each Solr collection
 * @param replicas Number of replicas for each Solr collection
 * @param rollPeriod Period (in days) to roll/rotate the Solr collections
 * @param solrConfigSetName Solr Config Set name. ConfigSet should exist in Zookeeper
 */
case class Application(name: String,
                       numCollections: Option[Int],
                       shards: Option[Int],
                       replicas: Option[Int],
                       rollPeriod: Option[Int],
                       solrConfigSetName: String = "testconf")
