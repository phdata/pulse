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

package io.phdata.pulse.alertengine.trigger

import io.phdata.pulse.alertengine.AlertRule
import io.phdata.pulse.common.SolrService

/**
 * Executes a solr query to determine if an alert should be triggered.
 *
 * @param solrService          Solr service used to run queries against solr collections
 */
class SolrAlertTrigger(solrService: SolrService) extends AbstractAlertTrigger {
  override def query(applicationName: String, alertRule: AlertRule): Seq[Map[String, Any]] = {
    val alias = s"${applicationName}_all"
    solrService.query(alias, alertRule.query)
  }
}
