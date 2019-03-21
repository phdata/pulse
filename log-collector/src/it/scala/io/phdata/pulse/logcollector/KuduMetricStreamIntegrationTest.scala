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

import io.phdata.pulse.common.domain.TimeseriesEvent
import org.apache.kudu.test.KuduTestHarness
import org.scalatest.{ BeforeAndAfterEach, FunSuite }

class KuduMetricStreamIntegrationTest extends FunSuite with BeforeAndAfterEach {
  val kuduTestHarness = new KuduTestHarness()

  override def beforeEach(): Unit = {
    super.beforeEach()
    kuduTestHarness.before()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    kuduTestHarness.after()
  }

  test("Create a table if it doesn't exist") {
    val client    = kuduTestHarness.getClient
    val stream    = new KuduMetricStream(kuduTestHarness.getClient)
    val tableName = "footable"

    assert(stream.tables.isEmpty)

    stream.getOrCreateTable(tableName)

    assert(client.tableExists(tableName))
  }

  test("Write events into Kudu") {
    val appName = "fooApp"
    val client  = kuduTestHarness.getClient
    val numRows = 1001

    val events = (1 to numRows).map { n =>
      new TimeseriesEvent(n, appName, 1.5d)
    }
    val stream = new KuduMetricStream(kuduTestHarness.getClient)
    events.foreach(e => stream.put(appName, e))

    // Sleep until the table is created, 'stream.put' runs asynchronously.
    while (!client.tableExists(stream.tableNameFromAppName(appName))) {
      Thread.sleep(100)
    }

    // Give some time for the row to be inserted
    Thread.sleep(10000)

    val table = client.openTable(stream.tableNameFromAppName(appName))
    val scanner = client
      .newScannerBuilder(table)
      .build()

    var rowCount = 0

    while (scanner.hasMoreRows) {
      rowCount = rowCount + scanner.nextRows().getNumRows
    }

    assertResult(numRows)(rowCount)
  }
}
