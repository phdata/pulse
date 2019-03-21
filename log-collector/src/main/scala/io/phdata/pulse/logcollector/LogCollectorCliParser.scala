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
  lazy val port        = opt[Int]("port", required = true, descr = "Listening port")
  lazy val zkHosts     = opt[String]("zk-hosts", required = true, descr = "Zookeeper hosts")
  lazy val kuduMasters = opt[String]("kudu-masters", required = false, descr = "Kudu masters")
  verify()
}
