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

package io.phdata.pulse.common.domain

/**
 * Describes the expected LogEvent input Object
 *
 * @param category   Fully qualified Class
 * @param timestamp  Timestamp
 * @param level      Logging Level
 * @param message    Log message
 * @param threadName Thread name
 * @param throwable  Throwable message
 * @param properties MDC properties
 */
case class LogEvent(id: Option[String],
                    category: String,
                    timestamp: String,
                    level: String,
                    message: String,
                    threadName: String,
                    throwable: Option[String] = None,
                    properties: Option[Map[String, String]] = None,
                    application: Option[String] = None)
