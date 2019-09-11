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

object KerberosContext extends LazyLogging {

  lazy private val loginContext = new LoginContext("Client")
  private var useKerberos       = false

  def scheduleKerberosLogin(initialDelay: Long, delay: Long, timeUnit: TimeUnit): Cancelable = {
    useKerberos = true
    val runnableLogin = new Runnable {
      def run(): Unit =
        login()
    }
    scheduler.scheduleWithFixedDelay(initialDelay, delay, timeUnit, runnableLogin)
  }

  def runPrivileged[W](work: => W): W =
    if (useKerberos) {
      Subject.doAs(
        loginContext.getSubject,
        new PrivilegedAction[W]() {
          override def run: W = {
            logger.debug("Privileged block started")
            val result = work
            logger.debug("Privileged block complete")
            result
          }
        }
      )
    } else {
      logger.debug("Kerberos disabled. To enable kerberos call the `scheduleKerberosLogin` method.")
      work
    }

  private def login(): Unit = {
    loginContext.login()
    logger.info(s"Logged in with kerberos configuration:\n$getSubject")
  }

  private def getSubject: String =
    if (loginContext.getSubject == null) {
      throw new Exception("Subject for LoginContext is null")
    } else {
      loginContext.getSubject.toString
    }
}
