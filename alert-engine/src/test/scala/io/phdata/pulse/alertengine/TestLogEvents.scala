
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


/**
  * Mother trait/interface for log events
  */
trait TestLogEvents

/**
  * Mother object for log events
  */
object TestLogEvents {

  def apply(categoryAndLevel: String): LogEvent = {
    categoryAndLevel match {
      case "alertError" => alertError
      case "errorError2" => errorError2
    }
  }

  /*
  *Factory methods
   */
  def alertError: LogEvent = {
    LogEvent(Some("id"),
      "ALERT",
      "1970-01-01T00:00:00Z",
      "ERROR",
      "message",
      "thread oxb",
      Some("Exception in thread main"),
      None)
  }

  def errorError2: LogEvent = {
    LogEvent(None,
      "ERROR",
      "1972-01-01T22:00:00Z",
      "ERROR2",
      "message2",
      "thread2 oxb",
      Some("Exception in thread main2"),
      None)
  }
}

