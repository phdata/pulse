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
import org.scalatest.FunSuite

class SparkLog4jExampleTest extends FunSuite {
  // TODO fix error: "com.fasterxml.jackson.databind.JsonMappingException: Incompatible Jackson version: 2.9.4"
  // Can be fixed temporarily by setting the jackson version to '2.2.3', but this needs to be fully tested with the rest of the app
  ignore("Run test job") {
    val conf = new SparkConf().setMaster("local[*]").setAppName("Pulse Spark Logging Example")
    val sc   = SparkContext.getOrCreate(conf)

    SparkLog4jExample.run(sc, 1)

    sc.stop()

  }
}
