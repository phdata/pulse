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

import java.sql.{ DriverManager, Statement }

import io.phdata.pulse.alertengine.{ AlertsDb, TestObjectGenerator }
import io.phdata.pulse.solr.TestUtil
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FunSuite }

class SqlAlertTriggerTest extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll {
  private val applicationName: String = "sql_test_" + TestUtil.randomIdentifier()
  private val dbUrl                   = s"jdbc:h2:mem:$applicationName;DB_CLOSE_DELAY=-1"

  override def beforeEach(): Unit = {
    super.beforeEach()
    AlertsDb.reset()
    prepareDatabase()
  }

  override def afterAll(): Unit =
    withStatement(statement => statement.execute("DROP ALL OBJECTS DELETE FILES;"))

  private def withStatement(function: Statement => Unit): Unit = {
    val connection = DriverManager.getConnection(dbUrl)
    try {
      val statement = connection.createStatement()
      try {
        function.apply(statement)
      } finally {
        statement.close()
      }
    } finally {
      connection.close()
    }
  }

  private def prepareDatabase(): Unit =
    withStatement { statement =>
      statement.execute("DROP ALL OBJECTS DELETE FILES;")
      statement.execute(s"""CREATE TABLE $applicationName (
           |id int not null,
           |error boolean not null,
           |message varchar(255) not null,
           |);""".stripMargin)
    }

  test("query returns matching documents") {
    withStatement { statement =>
      statement.execute(s"""INSERT INTO $applicationName (id, error, message) VALUES
           |(1, true, 'sad'),
           |(3, true, 'very sad'),
           |(2, false, 'happy');""".stripMargin)
    }
    val alertRule =
      TestObjectGenerator.alertRule(
        query = s"""select * from $applicationName
           |where error = true
           |order by id""".stripMargin,
        retryInterval = 1,
        resultThreshold = Some(1),
        alertProfiles = List("test@phdata.io")
      )
    val expectedDocuments = Seq(
      Map("id" -> 1, "error" -> true, "message" -> "sad"),
      Map("id" -> 3, "error" -> true, "message" -> "very sad")
    )

    val trigger = new SqlAlertTrigger(dbUrl)
    val result  = trigger.query(applicationName, alertRule)
    assertResult(expectedDocuments)(result)
  }

  test("query returns no documents") {
    val alertRule = TestObjectGenerator.alertRule(query = s"select * from $applicationName")

    val trigger = new SqlAlertTrigger(dbUrl)
    assertResult(Seq.empty)(trigger.query(applicationName, alertRule))
  }

  test("invalid query") {
    val alertRule = TestObjectGenerator.alertRule()

    val trigger = new SqlAlertTrigger(dbUrl)
    assertThrows[Exception](trigger.query(applicationName, alertRule))
  }

  test("connection with options") {
    val alertRule = TestObjectGenerator.alertRule(query = s"select * from $applicationName")

    val trigger = new SqlAlertTrigger(dbUrl, dbOptions = Map("hello" -> "stuff"))
    trigger.query(applicationName, alertRule)
  }

  test("dbUrl null") {
    assertThrows[IllegalArgumentException](new SqlAlertTrigger(null))
  }

  test("dbUrl empty") {
    assertThrows[IllegalArgumentException](new SqlAlertTrigger(""))
  }

}
