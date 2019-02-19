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

import io.phdata.pulse.metrics.Metrics
import org.apache.log4j.Logger
import org.apache.spark.{ SparkConf, SparkContext }

/**
 * This example shows how to
 * 1) initialize a logger
 * 2) write the application id and hostname to the driver and executors MDC so they will be
 * searchable in the solr index.
 */
object SparkLog4jExample {

  private implicit val log = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    log.info("Starting up the spark logging example")
    val conf = new SparkConf().setAppName("Pulse Spark Logging Example")
    val sc   = SparkContext.getOrCreate(conf)

    try {
      run(sc, args(0).toInt, args(1).toInt)
    } finally {
      sc.stop()
    }
  }

  // scalastyle:off
  def run(sc: SparkContext, numEvents: Int, step: Int): Unit = {
    val samples = 0 to numEvents by step
    Metrics.time("metric_l5") {
      samples.foreach { sampleNum =>
        println("start: " + sampleNum)
        val rdd = sc.parallelize(1 to sampleNum)

        rdd.filter { _ =>
          val x = math.random
          val y = math.random
          x * x + y * y < 1
        }
        val count = rdd.count()
        rdd.unpersist(true)
        println("done")

        log.info(s"Pi calculated to: ${4.0 * count / sampleNum} with $sampleNum samples")
      }
    }

    log.info("Shutting down the spark logging example")
    sc.stop()
  }

}
