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

package io.phdata.pulse.logcollector

import org.rogach.scallop.ScallopConf

class LogCollectorCliParser(args: Seq[String]) extends ScallopConf(args) {
  lazy val port        = opt[Int]("port", required = false, descr = "HTTP Server Listening port")
  lazy val zkHosts     = opt[String]("zk-hosts", required = true, descr = "Zookeeper hosts")
  lazy val kuduMasters = opt[String]("kudu-masters", required = false, descr = "Kudu masters")
  lazy val mode = opt[String]("consume-mode",
                              required = false,
                              descr = "'http' or 'kafka'",
                              default = Some("http"))
  lazy val kafkaProps =
    opt[String]("kafka-properties", required = false, descr = "Kafka properties file")
  lazy val topic = opt[String]("topic", required = false, descr = "Kafka Topic")

  validateOpt(mode, port) {
    case (Some("http") | None, None) => Left("Need a port if running http mode")
    case _                           => Right(Unit)
  }

  validateOpt(mode, kafkaProps, topic) {
    case (Some("kafka"), None, Some(_)) =>
      Left("--kafka-properties argument needed if --consume-mode=kafka")
    case (Some("kafka"), Some(_), None) => Left("--topic argument needed if --consume-mode=kafka")
    case (Some("kafka"), None, None) =>
      Left("--topic and --kafka-properties arguments needed if --consume-mode=kafka")
    case _ => Right(Unit)
  }

  verify()
}
