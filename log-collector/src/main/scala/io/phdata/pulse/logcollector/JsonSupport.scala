package io.phdata.pulse.logcollector

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.phdata.pulse.common.domain.{ LogEvent, TimeseriesEvent, TimeseriesRequest }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

/**
 * Provides Json serialization/deserialization for the LogEvent case class
 */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val logEventJsonFormat: RootJsonFormat[LogEvent] = jsonFormat9(LogEvent)
  implicit val timeseriesEventJsonFormat: RootJsonFormat[TimeseriesEvent] = jsonFormat4(
    TimeseriesEvent)
  implicit val timeseriesRequestJsonFormat: RootJsonFormat[TimeseriesRequest] = jsonFormat2(
    TimeseriesRequest)
}
