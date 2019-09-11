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
import org.apache.kudu.client.{ CreateTableOptions, KuduClient, KuduException, KuduTable }
import org.apache.kudu.{ ColumnSchema, Schema, Type }

import scala.collection.concurrent

object TimeseriesEventColumns {
  val TIMESTAMP = "ts"
  val KEY       = "key"
  val TAG       = "tag"
  val VALUE     = "value"
}

/**
 * A Kudu Metric stream batches data by time and record count into Kudu tables.
 * @param client
 */
class KuduMetricStream(client: KuduClient) extends Stream[TimeseriesEvent] {
  lazy val session = {
    val session = KerberosContext.runPrivileged(client.newSession())
    // Flushing is handled by the stream.
    session.setFlushMode(FlushMode.MANUAL_FLUSH)
    session
  }

  case class KuduRowErrorException(message: String) extends Exception(message)

  // Keep a cache of open tables.
  // TODO a bounded/timed cache. Is it possible for open tables to time out?
  private[logcollector] val tableCache = concurrent.TrieMap[String, KuduTable]()

  def close(): Unit =
    if (!session.isClosed) {
      KerberosContext.runPrivileged(session.close())
    }

  /**
   * Save a batch of records into the Kudu table
   * @param tableName The name of the application
   * @param metrics A sequence of metrics to write to Kudu
   */
  override private[logcollector] def save(tableName: String, metrics: Seq[TimeseriesEvent]): Unit =
    try {
      if (metrics.nonEmpty) {
        KerberosContext.runPrivileged {
          val table = getOrCreateTable(tableName)
          metrics.foreach { metric =>
            val insert = table.newInsert()
            val row    = insert.getRow
            row.addLong(TimeseriesEventColumns.TIMESTAMP, metric.ts)
            row.addString(TimeseriesEventColumns.KEY, metric.key)
            row.addString(TimeseriesEventColumns.TAG, metric.tag)
            row.addDouble(TimeseriesEventColumns.VALUE, metric.value)

            session.apply(insert)
          }

          session.flush()

          if (session.countPendingErrors() > 0) {
            val errors = session.getPendingErrors
            throw new KuduRowErrorException(errors.getRowErrors.head.toString)
          }

          logger.trace(s"Saved batch of ${metrics.length} to table $tableName")
        }
      }
    } catch {
      case e: KuduException =>
        logger.error(s"Exception writing to table $tableName, removing it from the cache.")
        KerberosContext.runPrivileged(tableCache.remove(tableName))
        throw e
    }

  /**
   * Get table from the cache if it doesn't exist, otherwise create it.
   * @param tableName The table name to retrieve or create
   * @return
   */
  private[logcollector] def getOrCreateTable(tableName: String): KuduTable =
    if (tableCache.contains(tableName)) {
      tableCache(tableName)
    } else if (!client.tableExists(tableName)) {
      logger.info(s"Kudu table not found: $tableName")
      val columns = new ArrayList[ColumnSchema]
      columns.add(new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventColumns.TIMESTAMP,
                                                       Type.UNIXTIME_MICROS).key(true).build)
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventColumns.KEY, Type.STRING)
          .key(true)
          .build)
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventColumns.TAG, Type.STRING)
          .key(true)
          .build)
      columns.add(
        new ColumnSchema.ColumnSchemaBuilder(TimeseriesEventColumns.VALUE, Type.DOUBLE)
          .key(false)
          .build)
      val schema = new Schema(columns)
      val opts = new CreateTableOptions()
        .setRangePartitionColumns(Collections.singletonList(TimeseriesEventColumns.TIMESTAMP))
        .addHashPartitions(Collections.singletonList(TimeseriesEventColumns.KEY), 4)
      val table = KerberosContext.runPrivileged(client.createTable(tableName, schema, opts))
      tableCache.put(tableName, table)
      logger.info(s"Created Kudu table $tableName")
      table
    } else {
      val table = client.openTable(tableName)
      tableCache.put(tableName, table)
      table
    }
}
