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
  implicit val alert        = yamlFormat4(AlertRule)
  implicit val mailProfile  = yamlFormat2(MailAlertProfile)
  implicit val slackProfile = yamlFormat2(SlackAlertProfile)
  implicit val application  = yamlFormat4(Application)
  implicit val config       = yamlFormat1(AlertEngineConfig)
}

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

case class AlertRule(query: String,
                     retryInterval: Int,
                     resultThreshold: Option[Int] = None,
                     alertProfiles: List[String])

trait AlertProfile {
  val name: String
}

case class SlackAlertProfile(name: String, url: String)            extends AlertProfile
case class MailAlertProfile(name: String, addresses: List[String]) extends AlertProfile
