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
  lazy val conf         = opt[String]("conf", 's', required = true)
  lazy val daemonize    = opt[Boolean]("daemonize", required = false, default = Some(false))
  lazy val smtpServer   = opt[String]("smtp-server", required = false)
  lazy val smtpUser     = opt[String]("smtp-user", required = false)
  lazy val smtpPassword = opt[String]("smtp-password", required = false, default = None)
  lazy val smtpPort     = opt[Long]("smtp-port", required = false)
  lazy val smtp_tls     = opt[Boolean]("smtp-tls", required = false)
  lazy val zkHosts      = opt[String]("zk-hosts", required = true)
  lazy val silencedApplicationsFile = opt[String](
    "silenced-application-file",
    required = false,
    descr = "File containing applications ignore when alerting, one application per line")

  verify()
}
