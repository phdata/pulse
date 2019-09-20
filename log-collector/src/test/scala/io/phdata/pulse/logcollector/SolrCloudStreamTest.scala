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

import org.scalatest.FunSuite
import scala.collection.mutable

class SolrCloudStreamTest extends FunSuite {
  test("should flush after n events") {
    val stream = new TestStream(1, 10)

    (1 to 10).foreach(x => stream.put("testApp", x.toString))
    Thread.sleep(2500)
    assert(stream.results.length === 10)
  }

  test("should flush after n seconds") {
    val stream = new TestStream(1, 10)

    (1 to 3).foreach(x => stream.put("testApp", x.toString))
    Thread.sleep(2500)
    assert(stream.results.length === 3)
  }
}

class TestStream(flushDuration: Int, flushSize: Int) extends Stream[String](StreamParams()) {
  StreamParams(1, 100, flushSize, flushDuration, "fail")
  var results: mutable.Seq[String] = mutable.Seq()

  override private[logcollector] def save(appName: String, values: Seq[String]): Unit =
    for (i <- values)
      results :+= i
}
