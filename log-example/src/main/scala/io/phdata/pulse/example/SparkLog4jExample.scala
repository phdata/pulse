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
import org.apache.log4j.MDC
import org.apache.spark.{ SparkConf, SparkContext }
import org.slf4j.LoggerFactory
import scala.util.Try

/**
 * This example shows how to
 * 1) initialize a logger
 * 2) write the application id and hostname to the driver and executors MDC so they will be
 * searchable in the solr index.
 */
object SparkLog4jExample {

  private val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    log.info("Starting up the spark logging example")
    val conf = new SparkConf().setAppName("Pulse Spark Logging Example")
    val sc   = SparkContext.getOrCreate(conf)

    val testData = 1 to 1000000
    val testRdd  = sc.parallelize(testData)

    // put the applicationId in a variable so it can be serialized and sent to the executors where [[sc]] is not available.
    val applicationId = sc.applicationId
    // set the application id of the driver
    org.apache.log4j.MDC.put("application_id", applicationId)

    /**
     * This lazy initializer will put the  hostname and application_id on the executors
     * Any values added to the MDC will be sent to the log collector and available for searching
     *
     */
    object LoggingConfiguration {
      lazy val init = {
        setMdcAppId
      }
      private lazy val setMdcAppId = Try(MDC.put("application_id", applicationId))
        .getOrElse(log.warn(s"Error setting application id"))
    }

    testRdd.foreach { num =>
      LoggingConfiguration.init // initialized once on first record, value is thrown away an nothing done for other records
      if (num % 100 == 0) {
        log.warn(s"found num: " + num)
      } else {
        log.info(s"error: " + num)
      }
    }

    log.info("Shutting down the spark logging example")
    sc.stop()
  }

}
