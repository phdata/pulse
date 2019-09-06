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

import org.rogach.scallop.{ ScallopConf, ScallopOption }

class AlertEngineCliParser(args: Seq[String]) extends ScallopConf(args) {
  lazy val conf: ScallopOption[String] = opt[String](
    "conf",
    's',
    required = true,
    descr =
      "Alert Engine config yaml file. See https://github.com/phdata/pulse/blob/master/alert-engine/README.md for schema")
  lazy val daemonize: ScallopOption[Boolean] = opt[Boolean](
    "daemonize",
    required = false,
    default = Some(false),
    descr = "Daemonize the process and run alerting on an interval")
  lazy val smtpServer: ScallopOption[String] =
    opt[String]("smtp-server", required = false, descr = "SMTP server hostmane")
  lazy val smtpUser: ScallopOption[String] = opt[String](
    "smtp-user",
    required = false,
    descr = "SMTP username (from address), like 'user@company.com'")

  // default smptPassword is "", this will turn Some("") into None()
  lazy val smtpPassword: Option[String] = sys.env.get("SMTP_PASSWORD").filter(_ != "")
  lazy val smtpPort: ScallopOption[Long] =
    opt[Long]("smtp-port", required = false, descr = "SMTP server port. Defaults to 25")
  lazy val smtp_tls: ScallopOption[Boolean] = opt[Boolean](
    "smtp-tls",
    required = false,
    descr = "Whether to use START_TLS. Defaults to false")
  lazy val silencedApplicationsFile: ScallopOption[String] = opt[String](
    "silenced-application-file",
    required = false,
    descr = "File containing applications ignore when alerting, one application per line")

  lazy val zkHost: ScallopOption[String] = opt[String](
    "zk-hosts",
    required = false,
    descr = "Zookeeper hosts. Used to connect to Solr Cloud")

  lazy val dbUrl: ScallopOption[String] =
    opt[String]("db-url", required = false, descr = "URL to connect to the database")
  lazy val dbUser: ScallopOption[String] =
    opt[String]("db-user", required = false, descr = "User to connect to the database as")
  lazy val dbPassword: ScallopOption[String] =
    opt[String]("db-password", required = false, descr = "Password to connect to the database with")
  lazy val dbOptions: ScallopOption[String] = opt[String](
    "db-options",
    required = false,
    descr = "Database connection options in the form `key1=value1;key2=value2`")

  verify()
}
