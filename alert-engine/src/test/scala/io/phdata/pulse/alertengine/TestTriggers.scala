
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

/**
  * Mother trait/interface class for alert Triggers
  */
trait TestTriggers

/**
  * Mother object alert Triggers
  */
object TestTriggers {


  def apply(triggerType: String): TriggeredAlert = {
    triggerType match {
      case "slackWithSparkAppAndTotalNumFound20" => slackWithSparkAppAndTotalNumFound20
      case "slackWithSparkAppAndTotalNumFound15" => slackWithSparkAppAndTotalNumFound15
      case "emailWithJavaAppAndTotalNumFound20" => emailWithJavaAppAndTotalNumFound20
      case "emailWithPythonAppAndTotalNumFound20" => emailWithPythonAppAndTotalNumFound20
      case "slackWithSparkAppAndTotalNumFound23" => slackWithSparkAppAndTotalNumFound23
      case "slackWithPipeWrenchAppAndTotalNumFound15" => slackWithPipeWrenchAppAndTotalNumFound15
      case "slackWithSparkAppAndTotalNumFound12" => slackWithSparkAppAndTotalNumFound12
      case "slackWithPipeWrenchAppAndTotalNumFound14" => slackWithPipeWrenchAppAndTotalNumFound14
      case "slackWithSparkAppWithNullDocAndTotalNumFound1" => slackWithSparkAppWithNullDocAndTotalNumFound1
      // case _                                            => slackWithSparkAppAndTotalNumFound20

    }
  }

  def slackWithSparkAppAndTotalNumFound20: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 20)
  }

  def emailWithJavaAppAndTotalNumFound20: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("emailWithRetryInterval10")
    TriggeredAlert(alertrule, "Java", Seq(doc), 20)
  }

  def emailWithPythonAppAndTotalNumFound20: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("emailWithRetryInterval10")
    TriggeredAlert(alertrule, "Python", Seq(doc), 20)
  }

  def slackWithSparkAppAndTotalNumFound23: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 23)
  }

  def slackWithSparkAppWithNullDocAndTotalNumFound1: TriggeredAlert = {
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", null, 1)
  }

  def slackWithPipeWrenchAppAndTotalNumFound15: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 15)
  }

  def slackWithSparkAppAndTotalNumFound12: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 12)
  }

  def slackWithSparkAppAndTotalNumFound15: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 15)
  }

  def slackWithPipeWrenchAppAndTotalNumFound14: TriggeredAlert = {
    val doc = TestSolrDocuments("fatal")
    val alertrule = TestAlertrules("slackWithRetryInterval10")
    TriggeredAlert(alertrule, "Spark", Seq(doc), 14)
  }

}
