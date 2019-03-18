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

import java.util.{ ArrayList, Collections }

import io.phdata.pulse.common.domain.TimeseriesEvent
import org.apache.kudu.client.SessionConfiguration.FlushMode
import org.apache.kudu.client.{ CreateTableOptions, KuduClient, KuduTable }
import org.apache.kudu.{ ColumnSchema, Schema, Type }

import scala.collection.concurrent

object TimeseriesEventConstants {
  val TIMESTAMP = "timestamp"
  val METRIC    = "metric"
  val VALUE     = "value"
}

class KuduMetricStream(client: KuduClient) extends Stream[TimeseriesEvent] {
  lazy val session = {
    val session = client.newSession()
    session.setFlushMode(FlushMode.MANUAL_FLUSH)
    session
  }

  val tables = concurrent.TrieMap[String, KuduTable]()

  def close(): Unit =
    if (!session.isClosed) {
      session.close()
    }

  override private[logcollector] def save(application: String,
                                          metrics: Seq[TimeseriesEvent]): Unit =
    if (metrics.nonEmpty) {
      val tableName = tableNameFromAppName(application)
      val table     = getOrCreateTable(tableName)
      metrics.foreach { metric =>
        val insert = table.newInsert()
        val row    = insert.getRow
        row.addLong(TimeseriesEventConstants.TIMESTAMP, metric.timestamp)
        row.addString(TimeseriesEventConstants.METRIC, metric.metric)
        row.addDouble(TimeseriesEventConstants.VALUE, metric.value)

        session.apply(insert)
      }

      session.flush()

      if (session.countPendingErrors() > 0) {
        val errors = session.getPendingErrors
        throw new Exception(errors.getRowErrors.head.toString)
      }
    }

  private[logcollector] def getOrCreateTable(tableName: String) = {
    if (!client.tableExists(tableName)) {
      logger.info(s"Kudu table not found: $tableName")
      val columns = new ArrayList[ColumnSchema]
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventConstants.TIMESTAMP,
                                             Type.UNIXTIME_MICROS).key(true).build)
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventConstants.METRIC, Type.STRING)
          .key(true)
          .build)
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventConstants.VALUE, Type.DOUBLE)
          .key(false)
          .build)
      val schema = new Schema(columns)
      val opts = new CreateTableOptions()
        .setRangePartitionColumns(Collections.singletonList(TimeseriesEventConstants.TIMESTAMP))
        .addHashPartitions(Collections.singletonList(TimeseriesEventConstants.METRIC), 4)
      val table = client.createTable(tableName, schema, opts)
      tables.put(tableName, table)
      logger.info(s"Created Kudu table $tableName")
    }
    tables(tableName)
  }

  private[logcollector] def tableNameFromAppName(application: String): String =
    s"pulse_$application"
}
