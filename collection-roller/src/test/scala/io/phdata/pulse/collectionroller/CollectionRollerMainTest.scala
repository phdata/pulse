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

import java.time.{ ZoneOffset, ZonedDateTime }

import io.phdata.pulse.common.SolrService
import io.phdata.pulse.testcommon.{ BaseSolrCloudTest, TestUtil }
import org.scalatest.FunSuite

class CollectionRollerMainTest extends FunSuite with BaseSolrCloudTest {
  var solrService =
    new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress,
                    miniSolrCloudCluster.getSolrClient)
  val collectionRoller = new CollectionRoller(solrService)

  val now = ZonedDateTime.now(ZoneOffset.UTC)

  test("List applications from Main method") {
    val app1Name = TestUtil.randomIdentifier()
    val app2Name = TestUtil.randomIdentifier()
    val args = Array(
      "--conf",
      "sample conf",
      "--zk-hosts",
      miniSolrCloudCluster.getZkServer.getZkAddress,
      "--list-applications"
    )

    val appList = List(
      Application(app1Name, None, None, None, None, "testconf"),
      Application(app2Name, None, None, None, None, "testconf")
    )
    collectionRoller.run(appList, now)
    assert(collectionRoller.collectionList().contains(app1Name))
    assert(collectionRoller.collectionList().contains(app2Name))
  }

  test("List applications from Main method in verbose mode") {
    val app1Name = TestUtil.randomIdentifier()
    val app2Name = TestUtil.randomIdentifier()
    val args = Array(
      "--conf",
      "sample conf",
      "--zk-hosts",
      miniSolrCloudCluster.getZkServer.getZkAddress,
      "--list-applications",
      "--verbose"
    )

    val appList = List(
      Application(app1Name, None, None, None, None, "testconf"),
      Application(app2Name, None, None, None, None, "testconf")
    )
    collectionRoller.run(appList, now)
    assert(collectionRoller.collectionList().contains(app1Name))
    assert(collectionRoller.collectionList().contains(app2Name))

    CollectionRollerMain.main(args)

    // check if alias exists for app1 & app2
    assert(collectionRoller.collectionList().contains(app1Name))
    assert(collectionRoller.collectionList().contains(app2Name))
  }

  test("delete applications from Main method") {
    val app1Name = TestUtil.randomIdentifier()
    val app2Name = TestUtil.randomIdentifier()
    val args = Array(
      "--conf",
      "sample conf",
      "--zk-hosts",
      miniSolrCloudCluster.getZkServer.getZkAddress,
      "--delete-applications",
      app1Name + "," + app2Name
    )

    val appList = List(
      Application(app1Name, None, None, None, None, "testconf"),
      Application(app2Name, None, None, None, None, "testconf")
    )

    collectionRoller.run(appList, now)
    CollectionRollerMain.main(args)

    // check if alias exists for app1 & app2
    assertResult(None)(solrService.getAlias(app1Name + "_latest"))
    assertResult(None)(solrService.getAlias(app1Name + "_all"))

    assertResult(None)(solrService.getAlias(app2Name + "_latest"))
    assertResult(None)(solrService.getAlias(app2Name + "_all"))

    // check if collections exist for app1 & app2
    assert(
      !solrService
        .listCollections()
        .contains(s"${app1Name}"))

    assert(
      !solrService
        .listCollections()
        .contains(s"${app2Name}"))
  }

}
