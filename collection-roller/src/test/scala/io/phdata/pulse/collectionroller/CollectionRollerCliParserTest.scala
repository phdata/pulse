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

class CollectionRollerCliParserTest extends FunSuite {

  test("Test Collection roller CLI ") {

    val args = Array(
      "--conf",
      "conf.yml",
      "--zk-hosts",
      "master1.valhalla.phdata.io/solr,master2.valhalla.phdata.io/solr,master3.valhalla.phdata.io/solr"
    )

    val collectionRollerCliparser = new CollectionRollerCliArgsParser(args)

    assertResult("conf.yml")(collectionRollerCliparser.conf())
    assertResult(
      "master1.valhalla.phdata.io/solr,master2.valhalla.phdata.io/solr,master3.valhalla.phdata.io/solr")(
      collectionRollerCliparser.zkHosts())

  }

}
