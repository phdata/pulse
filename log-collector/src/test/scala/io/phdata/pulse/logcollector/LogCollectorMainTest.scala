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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class LogCollectorMainTest extends FunSuite {
//  test("test recieiving multiple requests") {
//    val f = Future(LogCollector.main(null))
//    val document = new LogEvent(Some("id"),
//                                "ERROR",
//                                123,
//                                "ERROR",
//                                "message",
//                                "thread oxb",
//                                Some("Exception in thread main"),
//                                None)
//
//    Await.result(f, 6000 millis)
//  }

}
