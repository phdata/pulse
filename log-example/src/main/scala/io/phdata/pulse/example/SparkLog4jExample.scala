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

import org.apache.spark.{ SparkConf, SparkContext }
import org.slf4j.LoggerFactory

object SparkLog4jExample {

  private val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    log.info("Starting up the spark logging example")
    val conf = new SparkConf().setAppName("Pulse Spark Logging Example")
    val sc   = SparkContext.getOrCreate(conf)

    // set the application id on the Mapped Diagnostic Context so it will show as a field in the logs
    org.apache.log4j.MDC.put("application_id", sc.applicationId)

    val myTestData = 1 to 10000
    val testRdd    = sc.parallelize(myTestData)

    testRdd.foreach { num =>
      if (num % 100 == 0) {
        log.warn(s"found num: " + num)
      } else {
        log.info(s"error: " + num)
      }
    }

    log.info("Shutting down the spark logging example")
  }

}
