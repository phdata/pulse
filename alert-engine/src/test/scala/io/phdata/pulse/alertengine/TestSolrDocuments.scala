
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


import org.apache.solr.common.SolrDocument

/**
  * Mother trait/interface for solr documents
  */
trait TestSolrDocuments

/**
  * Mother object for solr documents
  */
object TestSolrDocuments {
  def apply(category: String): SolrDocument = {
    category.toUpperCase() match {
      case "ALL" => all
      case "DEBUG" => debug
      case "ERROR" => error
      case "FATAL" => fatal
      case "INFO" => info
      case "OFF" => off
      case "TRACE" => trace
      case "WARN" => warn
    }
  }

  /*
*Factory methods
 */
  def fatal: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "FATAL")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def error: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "ERROR")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def warn: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "WARN")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def trace: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "TRACE")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def debug: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "DEBUG")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def info: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "INFO")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def all: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "ALL")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

  def off: SolrDocument = {
    val doc: SolrDocument = new SolrDocument()
    doc.addField("id", "123")
    doc.addField("timestamp", "2018-04-06 10:15:00Z")
    doc.addField("level", "OFF")
    doc.addField("message", "The service is down.")
    doc.addField("threadName", "thread3")
    doc.addField("throwable", "NullPointerException")
    doc
  }

}

