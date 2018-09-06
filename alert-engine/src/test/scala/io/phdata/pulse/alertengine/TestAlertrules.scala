
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
  * Mother interface/factory for alert rules
  */
trait TestAlertrules

/**
  * Mother object ifor alert rules
  */
object TestAlertrules {

  def apply(ruleType: String): AlertRule = {
    ruleType match {
      case "defualtWithRetryInterval10" => defaultWithRetryInterval10
      case "emailWithRetryInterval10" => emailWithRetryInterval10
      case "slackWithRetryInterval10" => slackWithRetryInterval10
      case "slackWithRetryInterval20" => slackWithRetryInterval20
      case "emailWithRetryInterval1" => emailWithRetryInterval1
      case "emailWithRetryInterval1AndQueryIsNotexists" => emailWithRetryInterval1AndQueryIsNotexists
      case "emailWithRetryInterval1AndQueryIsId" => emailWithRetryInterval1AndQueryIsId
      case "emailWithRetryInterval1AndQueryIsSpark1query" => emailWithRetryInterval1AndQueryIsSpark1query
      case "emailWithRetryInterval1AndQueryIsSpark1query1" => emailWithRetryInterval1AndQueryIsSpark1query1
      case "emailWithRetryInterval1AndresultThreshold1" => emailWithRetryInterval1AndresultThreshold1
    }
  }

  /*
  *factory methods
   */

  def defaultWithRetryInterval10: AlertRule = {
    AlertRule("query0000000000", 10, Some(0), List("a", "slack"))

  }

  def emailWithRetryInterval10: AlertRule = {
    AlertRule("query0000000000", 10, Some(0), List("a", "email"))
  }

  def slackWithRetryInterval10: AlertRule = {
    AlertRule("query0000000000", 10, Some(0), List("a", "slack"))

  }

  def slackWithRetryInterval20: AlertRule = {
    AlertRule("query0000000000", 20, Some(0), List("a", "slack"))

  }

  def emailWithRetryInterval1: AlertRule = {
    AlertRule("category: DOESNOTEXIST", 1, Some(-1), List("tony@phdata.io"))

  }

  def emailWithRetryInterval1AndQueryIsNotexists: AlertRule = {
    AlertRule("id: notexists", 1, Some(0), List("tony@phdata.io"))
  }

  def emailWithRetryInterval1AndresultThreshold1: AlertRule = {
    AlertRule("category: DOESNOTEXIST", 1, Some(1), List("tony@phdata.io"))
  }

  def emailWithRetryInterval1AndQueryIsId: AlertRule = {
    AlertRule("id: id", 1, Some(0), List("mailprofile1"))
  }

  def emailWithRetryInterval1AndQueryIsSpark1query: AlertRule = {
    AlertRule("spark1query", 10, Some(10), List("mailprofile1"))
  }

  def emailWithRetryInterval1AndQueryIsSpark1query1: AlertRule = {
    AlertRule("spark2query1", 10, Some(10), List("mailprofile1"))
  }

  def slackWithRetryInterval1AndQueryIsId: AlertRule = {
    AlertRule("id: id", 1, Some(0), List("slackProfile1"))
  }

}

