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

package io.phdata.pulse

import io.phdata.pulse.log.{ HttpManager, JsonParser }
import monix.reactive.subjects.ConcurrentSubject
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import org.apache.log4j.helpers.LogLog
import org.apache.log4j.spi.LoggingEvent

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success, Try }

abstract class Stream[E](flushDuration: FiniteDuration, flushSize: Int, maxBuffer: Int) {

  val overflowStragegy = OverflowStrategy.DropNewAndSignal(maxBuffer, (_: Long) => None)
  val subject          = ConcurrentSubject.publish[E](overflowStragegy)

  subject
    .bufferTimedAndCounted(flushDuration, flushSize)
    .map(save)
    .subscribe()

  def append(value: E): Unit =
    Try { subject.onNext(value) } match {
      case Success(_) => ()
      case Failure(e) => LogLog.error("Error appending to stream", e)
    }

  def save(values: Seq[E])

}

class HttpStream(flushDuration: FiniteDuration,
                 flushSize: Int,
                 maxBuffer: Int,
                 httpManager: HttpManager)
    extends Stream[LoggingEvent](flushDuration, flushSize, maxBuffer) {

  val jsonParser = new JsonParser

  override def save(values: Seq[LoggingEvent]): Unit = {
    val logArray = values.toArray
    LogLog.debug(s"Flushing ${logArray.length} messages")
    val logMessage = jsonParser.marshallArray(logArray)

    httpManager.send(logMessage)
  }
}
