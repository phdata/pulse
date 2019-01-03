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

package io.phdata.pulse.collectionroller

import org.scalatest.FunSuite

class ConfigParserTest extends FunSuite {
  test("parse config") {
    val yaml =
      """---
        |solrConfigSetDir: conf
        |applications:
        |- name: application1
        |  numCollections: 2
        |  shards: 1
        |  replicas: 1
        |  rollPeriod: 1
        |  solrConfigSetName: config1
        |  """.stripMargin

    val expected =
      CollectionRollerConfig(
        "conf",
        List(Application("application1", Some(2), Some(1), Some(1), Some(1), "config1")))

    assertResult(expected)(ConfigParser.convert(yaml))
  }
}
