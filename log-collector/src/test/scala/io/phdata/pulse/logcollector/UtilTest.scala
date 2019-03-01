package io.phdata.pulse.logcollector

import io.phdata.pulse.common.domain.LogEvent
import org.scalatest.FunSuite

class UtilTest extends FunSuite {

  val document1 = new LogEvent(None,
                               "ERROR",
                               "1970-01-01T00:00:00Z",
                               "ERROR",
                               "message 1",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  val document2 = new LogEvent(None,
                              "ERROR",
                              "1970-01-01T00:00:00Z",
                              "ERROR",
                              "message 1",
                              "thread oxb",
                              Some("Exception in thread main"),
                              Some(Map("property1" -> "15", "property2" -> "true")),
                              None)

  test("Test Util.logEventToFlattenedMap converts LogEvent to Map[String, String]") {

    val parsedLogEventMap = Util.logEventToFlattenedMap(document1)

    assert(parsedLogEventMap.isInstanceOf[Map[String, String]])

  }

  test("Test Util.logEventToFlattenedMap converts LogEvent with properties to Map[String, String]") {

    val parsedLogEventMap = Util.logEventToFlattenedMap(document2)

    assert(parsedLogEventMap.isInstanceOf[Map[String, String]])

  }
}
