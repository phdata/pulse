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

package io.phdata.pams.example

import java.net.InetAddress

import org.apache.spark.{ SparkConf, SparkContext }
import org.slf4j.LoggerFactory

import scala.util.Try

object SparkLog4jExample {

  private val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    log.info("Starting up the spark logging example")
    val conf = new SparkConf().setAppName("Pulse Spark Logging Example")
    val sc   = SparkContext.getOrCreate(conf)

    val myTestData    = 1 to 10000
    val testRdd       = sc.parallelize(myTestData)
    val applicationId = sc.applicationId

    // set the application id of the driver
    org.apache.log4j.MDC.put("application_id", applicationId)
    // set the hostname of the driver
    org.apache.log4j.MDC.put("hostname", InetAddress.getLocalHost().getHostName())

    /**
     * This lazy initializer will set hostname and application_id on the executors
     */
    object LoggingConfiguration {
      private lazy val setMdcAppId = Try(org.apache.log4j.MDC.put("application_id", applicationId))
        .getOrElse(log.warn(s"Error setting application id"))
      private lazy val setHostName = Try(
        org.apache.log4j.MDC.put("hostname", InetAddress.getLocalHost().getHostName()))
      lazy val init = {
        setHostName
        setMdcAppId
      }
    }

    testRdd.foreach { num =>
      LoggingConfiguration.init // initialized once on first record, value is thrown away an nothing done for other records
      if (num % 100 == 0) {
        log.warn(s"found num: " + num)
      } else {
        log.info(s"error: " + num)
      }
    }

    val y = testRdd.mapPartitions { p =>
      LoggingConfiguration.init // initialized once on first record, value is thrown away an nothing done for other records
      p.map(x => x)
    }

    log.info("Shutting down the spark logging example")
  }

}
