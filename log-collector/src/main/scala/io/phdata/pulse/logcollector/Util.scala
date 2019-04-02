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

import io.phdata.pulse.common.domain.LogEvent

object Util {

  /**
   * Convert a [[LogEvent]] to [[Map[String,String]] by flattening the nested properties field
   * @param logEvent LogEvent to convert
   * @return The event, with properties flattened, as a [[Map[String,String]]
   */
  def logEventToFlattenedMap(logEvent: LogEvent): Map[String, String] = {
    val rawMap = Map(
      "category"    -> logEvent.category,
      "timestamp"   -> logEvent.timestamp,
      "level"       -> logEvent.level,
      "message"     -> logEvent.message,
      "threadName"  -> logEvent.threadName,
      "throwable"   -> logEvent.throwable.getOrElse(""),
      "application" -> logEvent.application.getOrElse("")
    )

    logEvent.properties match {
      case Some(properties) => rawMap ++ properties
      case None             => rawMap
    }
  }
}
