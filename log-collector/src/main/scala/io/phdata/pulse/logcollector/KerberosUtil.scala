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

import java.security.PrivilegedAction
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import javax.security.auth.Subject
import javax.security.auth.login.LoginContext
import monix.execution.Cancelable
import monix.execution.Scheduler.{ global => scheduler }

object KerberosUtil extends LazyLogging {

  private var lc = new LoginContext("Client")

  def scheduledLogin(initialDelay: Long, delay: Long, timeUnit: TimeUnit): Cancelable = {
    val runnableLogin = new Runnable {
      def run(): Unit = {
        lc = new LoginContext("Client")
        login()
      }
    }
    scheduler.scheduleWithFixedDelay(initialDelay, delay, timeUnit, runnableLogin)
  }

  def run[F](function: => Any): Any =
    Subject.doAs(lc.getSubject, new PrivilegedAction[Any]() {
      override def run: Any = function
    })

  def login(): Unit = {
    lc.login()
    logger.info(s"Logging in with kerberos configuration:\n$getSubject")
  }

  def getSubject: String =
    if (lc.getSubject == null) {
      throw new Exception("Subject for LoginContext is null")
    } else {
      lc.getSubject.toString
    }
}
