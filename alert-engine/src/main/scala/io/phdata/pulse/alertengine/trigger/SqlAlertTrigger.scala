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

package io.phdata.pulse.alertengine.trigger

import java.sql.{ DriverManager, ResultSet, ResultSetMetaData }
import java.util.Properties

import io.phdata.pulse.alertengine.AlertRule

case class ConnectionInfo(url: String, properties: Properties)

object SqlAlertTrigger {
  val MAX_ROWS = 10
}

/**
 * Executes an SQL query to determine if an alert should be triggered.
 * @param dbUrl the database url
 * @param dbUser an optional database user
 * @param dbPassword an optional database password
 * @param dbOptions additional database options
 */
class SqlAlertTrigger(val dbUrl: String,
                      val dbUser: Option[String] = None,
                      val dbPassword: Option[String] = None,
                      val dbOptions: Map[String, String] = Map.empty)
    extends AbstractAlertTrigger {

  if (dbUrl == null || dbUrl.trim.isEmpty) {
    throw new IllegalArgumentException("dbUrl must be defined")
  }

  private def dbProperties: Properties = {
    val props = new Properties()
    dbUser.map(props.put("user", _))
    dbPassword.map(props.put("password", _))
    dbOptions.foreach { case (key, value) => props.put(key, value) }
    props
  }

  override def query(applicationName: String, alertRule: AlertRule): Seq[Map[String, Any]] = {
    val resultBuilder = List.newBuilder[Map[String, Any]]
    val connection    = DriverManager.getConnection(dbUrl, dbProperties)
    try {
      val statement = connection.createStatement()
      statement.closeOnCompletion()
      statement.setMaxRows(SqlAlertTrigger.MAX_ROWS)
      val resultSet = statement.executeQuery(alertRule.query)
      try {
        val md = resultSet.getMetaData
        while (resultSet.next()) {
          val doc = convertToMap(resultSet, md)
          resultBuilder += doc
        }
      } finally {
        resultSet.close()
      }
    } finally {
      connection.close()
    }
    resultBuilder.result
  }

  /**
   * Converts the current row to a Map.
   * @param results the result set
   * @param md the result set metadata
   * @return the current row as a Map
   */
  private def convertToMap(results: ResultSet, md: ResultSetMetaData): Map[String, Any] =
    (1 to md.getColumnCount)
      .map(i => md.getColumnName(i).toLowerCase -> results.getObject(i))
      .toMap

}
