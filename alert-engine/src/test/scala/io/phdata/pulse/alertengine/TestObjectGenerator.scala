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

import io.phdata.pulse.common.domain.LogEvent
import org.apache.solr.common.SolrDocument

/**
 * TestObjectGenerator helps avoid redundant test object creation by providing methods
 * for generating default and custom objects for testing.
 *
 */
object TestObjectGenerator {

  /**
   * Method used for generating default and custom solr document test objects
   * based on parameters passed : if no parameter is passed it returns a default solr document.
   *
   * @param id
   * @param category
   * @param timestamp
   * @param level
   * @param message
   * @param threadName
   * @param throwable
   * @return solrDocument
   */
  def solrDocument(
      id: String = "123",
      category: String = "test",
      timestamp: String = "2018-04-06 10:15:00",
      level: String = "FATAL",
      message: String = "The service is down.",
      threadName: String = "thread3",
      throwable: String = "NullPointerException"
  ): SolrDocument = {

    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", id)
    doc.addField("category", category)
    doc.addField("timestamp", timestamp)
    doc.addField("level", level)
    doc.addField("message", message)
    doc.addField("threadName", threadName)
    doc.addField("throwable", throwable)

    doc
  }

  /**
   * Method for creating logEvent test ojects
   *
   * @param id
   * @param category
   * @param timestamp
   * @param level
   * @param message
   * @param threadName
   * @param throwable
   * @param properties
   * @return LogEvent
   */
  def logEvent(id: Option[String] = Some("id"),
               category: String = "ALERT",
               timestamp: String = "1970-01-01T00:00:00Z",
               level: String = "ERROR",
               message: String = "message",
               threadName: String = "thread oxb",
               throwable: Option[String] = Some("Exception in thread main"),
               properties: Option[Map[String, String]] = None): LogEvent =
    LogEvent(id, category, timestamp, level, message, threadName, throwable, properties)

  /**
   * Method for creating test mail alert profiles
   *
   * @param name
   * @param addresses
   * @return MailAlertProfile
   */
  def mailAlertProfile(name: String = "mailprofile1",
                       addresses: List[String] = List("person@phdata.io")): MailAlertProfile =
    MailAlertProfile(name, addresses)

  /**
   * Method for creating test slack alert profiles
   *
   * @param name
   * @param url
   * @return SlackAlertProfile
   */
  def slackAlertProfile(name: String = "slackProfile1",
                        url: String = "testurl.com"): SlackAlertProfile =
    SlackAlertProfile(name, url)

  /**
   * Method for creating test alert rules
   *
   * @param query
   * @param retryInterval
   * @param resultThreshold
   * @param alertProfiles
   * @return AlertRule
   */
  def alertRule(query: String = "id : testId",
                retryInterval: Int = 10,
                resultThreshold: Option[Int] = None,
                alertProfiles: List[String] = List("mailprofile1")): AlertRule =
    AlertRule(query, retryInterval, resultThreshold, alertProfiles)

  /**
   * Method for creating test triggered alerts
   *
   * @param rule
   * @param applicationName
   * @param documents
   * @param totalNumFound
   * @return TriggeredAlert
   */
  def triggeredAlert(rule: AlertRule = alertRule(),
                     applicationName: String = "Spark",
                     documents: Seq[SolrDocument] = Seq(solrDocument()),
                     totalNumFound: Long = 20): TriggeredAlert =
    TriggeredAlert(rule, applicationName, documents, totalNumFound)
}
