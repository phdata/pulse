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

import org.rogach.scallop.ScallopConf

class AlertEngineCliParser(args: Seq[String]) extends ScallopConf(args) {
  lazy val conf = opt[String](
    "conf",
    's',
    required = true,
    descr =
      "Alert Engine config yaml file. See https://github.com/phdata/pulse/blob/master/alert-engine/README.md for schema")
  lazy val daemonize = opt[Boolean]("daemonize",
                                    required = false,
                                    default = Some(false),
                                    descr = "Daemonize the process and run alerting on an interval")
  lazy val smtpServer = opt[String]("smtp-server", required = false, descr = "SMTP server hostmane")


  lazy val smtpUser = opt[String]("smtp-user",
                                  required = false,
                                  descr = "SMTP username (from address), like 'user@company.com'")

  lazy val smtpPassword =  sys.env.getOrElse("SMTP_PASSWORD", "None")


  lazy val smtpPort =
    opt[Long]("smtp-port", required = false, descr = "SMTP server port. Defaults to 25")
  lazy val smtp_tls = opt[Boolean]("smtp-tls",
                                   required = false,
                                   descr = "Whether to use START_TLS. Defaults to false")
  lazy val zkHost = opt[String]("zk-hosts",
                                required = true,
                                descr = "Zookeeper hosts. Used to connect to Solr Cloud")
  lazy val silencedApplicationsFile = opt[String](
    "silenced-application-file",
    required = false,
    descr = "File containing applications ignore when alerting, one application per line")

  verify()
}
