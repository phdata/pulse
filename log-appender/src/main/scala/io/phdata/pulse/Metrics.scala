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

import org.apache.log4j.Logger

/**
 * Write metric tags (key-value pairs) to the Mapped Diagnostic Context. These key value pairs
 * are then pushed into Solr by Pulse where they can be graphed or queried.
 *
 * Most Solr schemas created for Pulse indexes will have dynamic properties set up, so
 * it's not necessary to create a field. To use a dynamic field, suffix your tag with:
 * _s: String
 * _i: Integer
 * _l: Long
 * _f: Float
 * _d: Double
 * Anything else: String
 *
 * Using dynamic fields does not have a large performance impact, but not all visualization tools
 * will be able to recognize dynamic fields.
 */
object Metrics {
  import org.apache.log4j.MDC

  /**
   * Time the result of a computation and store the result
   * @param tag Identifier tag
   * @param func Function to be timed
   * @param logger Logger to be used
   * @tparam E The function return type
   * @return
   */
  def time[E](tag: String)(func: => E)(implicit logger: Logger): E = {
    val start   = System.currentTimeMillis()
    val result  = func
    val elapsed = System.currentTimeMillis() - start
    gauge(tag, elapsed)(logger)
    result
  }

  /**
   * Write a gauge of `String` type with the given tag.
   * @param tag Identifier tag of the gauge
   * @param value Value of the gauge
   * @param logger Logger to place the message on
   */
  def gauge(tag: String, value: String)(implicit logger: Logger): Unit = {
    MDC.put(tag, value)
    logger.info(s"metric: $tag=$value")
    MDC.remove(tag)
  }

  /**
   * Write a gauge of `Long` type with the given tag.
   * @param tag Identifier tag of the gauge
   * @param value Value of the gauge
   * @param logger Logger to place the message on
   */
  def gauge(tag: String, value: Long)(implicit logger: Logger): Unit =
    gauge(tag, value.toString)(logger)

  /**
   * Write a gauge of `Int` type with the given tag.
   * @param tag Identifier tag of the gauge
   * @param value Value of the gauge
   * @param logger Logger to place the message on
   */
  def gauge(tag: String, value: Int)(implicit logger: Logger): Unit =
    gauge(tag, value.toString)(logger)

  /**
   * Write a gauge of `Double` type with the given tag.
   * @param tag Identifier tag of the gauge
   * @param value Value of the gauge
   * @param logger Logger to place the message on
   */
  def gauge(tag: String, value: Double)(implicit logger: Logger): Unit =
    gauge(tag, value.toString)(logger)

  /**
   * Write a gauge of `Float` type with the given tag.
   * @param tag Identifier tag of the gauge
   * @param value Value of the gauge
   * @param logger Logger to place the message on
   */
  def gauge(tag: String, value: Float)(implicit logger: Logger): Unit =
    gauge(tag, value.toString)(logger)
}
