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

import java.io.FileNotFoundException
import java.time.{ ZoneOffset, ZonedDateTime }

import io.phdata.pulse.common.SolrService
import io.phdata.pulse.testcommon.{ BaseSolrCloudTest, TestUtil }
import org.scalatest._

class CollectionRollerTest extends FunSuite with BaseSolrCloudTest {

  val solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

  test("create an application if it doesn't exist") {
    val appName = TestUtil.randomIdentifier()

    val now              = ZonedDateTime.now(ZoneOffset.UTC)
    val nowSeconds       = now.toInstant.getEpochSecond
    val collectionRoller = new CollectionRoller(solrService, now)
    val app              = Application(appName, None, None, None, None, "testconf")
    collectionRoller.run(List(app))

    assertResult(true)(solrService.collectionExists(s"${app.name}_$nowSeconds"))
    assertResult(true)(solrService.aliasExists(s"${app.name}_latest"))
    assertResult(true)(solrService.aliasExists(s"${app.name}_all"))
  }

  test("upload solr configs") {
    val collectionRoller = new CollectionRoller(solrService, ZonedDateTime.now(ZoneOffset.UTC))

    collectionRoller.uploadConfigsFromDirectory("./test-config")
  }

  test("fail if config dir is null") {
    val collectionRoller = new CollectionRoller(solrService, ZonedDateTime.now(ZoneOffset.UTC))

    assert(collectionRoller.uploadConfigsFromDirectory(null).isFailure)

  }

  test("fail if config dir does not exist") {
    val collectionRoller = new CollectionRoller(solrService, ZonedDateTime.now(ZoneOffset.UTC))

    assert(collectionRoller.uploadConfigsFromDirectory("faksjhdfaksjdfh").isFailure)
  }

  test("oldest collection is deleted") {
    val appName     = TestUtil.randomIdentifier()
    val initialTime = ZonedDateTime.now(ZoneOffset.UTC)
    val secondTime  = initialTime.plusDays(1)
    val thirdTime   = initialTime.plusDays(2)
    val fourthTime  = initialTime.plusDays(3)
    val fifthTime   = initialTime.plusDays(4)

    val collectionRoller = new CollectionRoller(solrService, initialTime)

    val app =
      Application(appName, Some(2), Some(1), Some(1), Some(1), "testconf")
    collectionRoller.run(List(app))
    collectionRoller.rollApplications(List(app))

    val times = List(secondTime, thirdTime, fourthTime, fifthTime)

    times foreach { timestamp =>
      val collectionRoller =
        new CollectionRoller(solrService, ZonedDateTime.parse(timestamp.toString))
      collectionRoller.rollApplications(List(app))
    }

    assert(
      !solrService
        .listCollections()
        .contains(s"${app.name}_${initialTime.toInstant.getEpochSecond}"))
    assert(
      !solrService
        .listCollections()
        .contains(s"${app.name}_${secondTime.toInstant.getEpochSecond}"))
    assert(
      !solrService.listCollections().contains(s"${app.name}_${thirdTime.toInstant.getEpochSecond}"))
    assert(
      solrService.listCollections().contains(s"${app.name}_${fourthTime.toInstant.getEpochSecond}"))
    assert(
      solrService.listCollections().contains(s"${app.name}_${fifthTime.toInstant.getEpochSecond}"))

    assertResult(
      Some(Set(s"${app.name}_${fifthTime.toInstant.getEpochSecond}",
               s"${app.name}_${fourthTime.toInstant.getEpochSecond}")))(
      solrService.getAlias(s"${app.name}_all"))
  }

  test("don't roll collection if a day hasn't passed") {
    val appName     = TestUtil.randomIdentifier()
    val initialTime = ZonedDateTime.now(ZoneOffset.UTC)
    val secondTime  = initialTime.plusMinutes(60)

    val collectionRoller = new CollectionRoller(solrService, initialTime)

    val app =
      Application(appName, None, None, None, None, "testconf")
    collectionRoller.run(List(app))

    collectionRoller.rollApplications(List(app))

    val collectionRoller2 =
      new CollectionRoller(solrService, ZonedDateTime.parse(secondTime.toString))
    collectionRoller2.rollApplications(List(app))

    assert(
      solrService
        .listCollections()
        .contains(s"${app.name}_${initialTime.toInstant.getEpochSecond}"))

    assertResult(Some(Set(s"${app.name}_${initialTime.toInstant.getEpochSecond}")))(
      solrService.getAlias(s"${app.name}_all"))
  }

  test("move latest alias collection") {
    val appName     = TestUtil.randomIdentifier()
    val initialTime = ZonedDateTime.now(ZoneOffset.UTC)
    val secondTime  = initialTime.plusDays(2)

    val collectionRoller = new CollectionRoller(solrService, initialTime)

    val app =
      Application(appName, None, None, None, None, "testconf")
    collectionRoller.run(List(app))

    collectionRoller.rollApplications(List(app))

    val collectionRoller2 =
      new CollectionRoller(solrService, ZonedDateTime.parse(secondTime.toString))
    collectionRoller2.rollApplications(List(app))

    assertResult(Some(Set(s"${app.name}_${secondTime.toInstant.getEpochSecond}")))(
      solrService.getAlias(s"${app.name}_latest"))
  }

  test("delete application") {
    val app1Name    = TestUtil.randomIdentifier()
    val app2Name    = TestUtil.randomIdentifier()
    val initialTime = ZonedDateTime.now(ZoneOffset.UTC)
    val app1 =
      Application(app1Name, None, None, None, None, "testconf")

    val app2 =
      Application(app2Name, None, None, None, None, "testconf")

    val collectionRoller = new CollectionRoller(solrService, initialTime)

    collectionRoller.run(List(app1, app2))
    collectionRoller.deleteApplications(List(app1Name, app2Name))

    // check if alias exists for app1 & app2
    assertResult(None)(solrService.getAlias(app1Name + "_latest"))
    assertResult(None)(solrService.getAlias(app1Name + "_all"))

    assertResult(None)(solrService.getAlias(app2Name + "_latest"))
    assertResult(None)(solrService.getAlias(app2Name + "_all"))

    // check if collections exist for app1 & app2
    assert(
      !solrService
        .listCollections()
        .contains(s"${app1.name}_${initialTime.toInstant.getEpochSecond}"))

    assert(
      !solrService
        .listCollections()
        .contains(s"${app2.name}_${initialTime.toInstant.getEpochSecond}"))
  }
}
