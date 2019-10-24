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

package io.phdata.pulse.metrics

import java.util.concurrent.TimeUnit

import io.phdata.pulse.Stream
import org.scalatest.FunSuite

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}

class TestStream(flushDuration: FiniteDuration, flushSize: Int) extends Stream[String](flushDuration, flushSize) {
  var results: mutable.Seq[String] = mutable.Seq()

  override def save(values: Seq[String]): Unit =
    for (i <- values)
      results :+= i
}

class StreamTest extends FunSuite {

  test("should flush after n events") {
    val stream = new TestStream(Duration(1,TimeUnit.SECONDS), 10)

    (1 to 10).foreach(x => stream.append((x.toString)))
    Thread.sleep(2500)
    assert(stream.results.length === 10)
  }

  test("should flush after n second") {
    val stream = new TestStream(Duration(1,TimeUnit.SECONDS), 10)

    (1 to 3).foreach(x => stream.append((x.toString)))
    Thread.sleep(2000)
    assert(stream.results.length === 3)
  }
}
