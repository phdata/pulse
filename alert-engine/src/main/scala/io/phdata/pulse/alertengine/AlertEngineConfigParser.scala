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

package io.phdata.pulse.alertengine

import net.jcazevedo.moultingyaml._

import scala.io.Source

object YamlProtocol extends DefaultYamlProtocol {
  implicit val alert: YamlFormat[AlertRule] = yamlFormat4(AlertRule)
  implicit val mailProfile: YamlFormat[MailAlertProfile] = yamlFormat2(MailAlertProfile)
  implicit val slackProfile: YamlFormat[SlackAlertProfile] = yamlFormat2(SlackAlertProfile)
  implicit val application: YamlFormat[Application] = yamlFormat4(Application)
  implicit val config: YamlFormat[AlertEngineConfig] = yamlFormat1(AlertEngineConfig)
}

/**
  * Methods to parse an alert engine yaml into case classes
  */
object AlertEngineConfigParser {
  def getConfig(path: String): AlertEngineConfig = {
    val yamlString = Source.fromFile(path).getLines.mkString("\n")
    convert(yamlString)
  }

  def convert(yaml: String): AlertEngineConfig = {
    import YamlProtocol._
    yaml.parseYaml.convertTo[AlertEngineConfig]
  }
}

case class AlertEngineConfig(applications: List[Application])

case class Application(name: String,
                       alertRules: List[AlertRule],
                       emailProfiles: Option[List[MailAlertProfile]],
                       slackProfiles: Option[List[SlackAlertProfile]])

/**
  * An [[AlertRule]]
  * @param query Solr Query that acts as a predicate, for example this query:
  *              {{{timestamp:[NOW-10MINUTES TO NOW] AND level: ERROR}}} will alert if any message
  *              with level 'ERROR' is found within the last 10 minutes
  * @param retryInterval the query will be run on the retry interval. The retry interval is set in minutes
  * @param resultThreshold if the query returns more than threshold results, an alert will be
  *                        triggered. The default is 0. If the threshold is set to `-1`, the
  *                        non-existence of documents with this query will trigger an alert. This
  *                        is useful for tracking application uptime.
  * @param alertProfiles One or many alertProfiles can be defined. For each alertProfile defined in
  *                      an alert, an alertProfile needs to be defined for the application
  */
case class AlertRule(query: String,
                     retryInterval: Int,
                     resultThreshold: Option[Int] = None,
                     alertProfiles: List[String])

trait AlertProfile {
  val name: String
}

case class SlackAlertProfile(name: String, url: String)            extends AlertProfile

case class MailAlertProfile(name: String, addresses: List[String]) extends AlertProfile
