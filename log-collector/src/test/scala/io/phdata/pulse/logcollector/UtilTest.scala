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
import org.scalatest.FunSuite

class UtilTest extends FunSuite {

  val document1 = new LogEvent(None,
                               "ERROR",
                               "1970-01-01T00:00:00Z",
                               "ERROR",
                               "message 1",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  val document2 = new LogEvent(
    None,
    "ERROR",
    "1970-01-01T00:00:00Z",
    "ERROR",
    "message 1",
    "thread oxb",
    Some("Exception in thread main"),
    Some(Map("property1" -> "15", "property2" -> "true")),
    None
  )

  test("Test Util.logEventToFlattenedMap converts LogEvent to Map[String, String]") {

    val parsedLogEventMap = Util.logEventToFlattenedMap(document1)

    assert(parsedLogEventMap.isInstanceOf[Map[String, String]])

  }

  test("Test Util.logEventToFlattenedMap converts LogEvent with properties to Map[String, String]") {

    val parsedLogEventMap = Util.logEventToFlattenedMap(document2)

    assert(parsedLogEventMap.isInstanceOf[Map[String, String]])

  }
}
