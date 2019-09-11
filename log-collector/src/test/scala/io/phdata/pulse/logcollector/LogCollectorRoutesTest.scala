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

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, MessageEntity, StatusCodes }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.phdata.pulse.common.JsonSupport
import io.phdata.pulse.common.domain.{ LogEvent, TimeseriesEvent, TimeseriesRequest }
import io.phdata.pulse.solr.BaseSolrCloudTest
import org.mockito.Mockito.verify
import org.mockito.{ Matchers, Mockito }
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar
import spray.json._
import org.mockito.Mockito.when

class LogCollectorRoutesTest
    extends FunSuite
    with ScalatestRouteTest
    with JsonSupport
    with BaseSolrCloudTest
    with MockitoSugar {

  val document = new LogEvent(None,
                              "ERROR",
                              "1970-01-01T00:00:00Z",
                              "ERROR",
                              "message",
                              "thread oxb",
                              Some("Exception in thread main"),
                              None)

  val jsonArrayDocument: String =
    """[
      |{ "timestamp": "1970-01-01T00:00:00Z",
      | "category": "ERROR",
      | "message": "message",
      | "threadName": "thread oxb",
      | "throwable": "Exception in thread main",
      | "level": "ERROR"
      | },
      | { "timestamp": "1970-01-01T00:00:00Z",
      | "category": "ERROR",
      | "message": "message",
      | "threadName": "thread oxb",
      | "throwable": "Exception in thread main",
      | "level": "ERROR"
      | }
      | ]""".stripMargin

  val solrStream = mock[SolrCloudStream]
  val kuduService = mock[KuduService]

  implicit val actorSystem: ActorSystem = ActorSystem()

  val routes = new LogCollectorRoutes(solrStream, Some(kuduService)).routes

  test("post json to endpoint") {
    val docEntity = Marshal(document).to[MessageEntity].futureValue

    Post(uri = "/log?application=test")
      .withEntity(docEntity) ~> routes ~> check {
      assert(status === (StatusCodes.OK))
    }

    verify(solrStream).put(Matchers.anyString(), Matchers.any[Map[String, String]]())
  }

  test("post single log event to 'event' endpoint") {
    val docEntity = Marshal(document).to[MessageEntity].futureValue

    Post(uri = "/v2/event/test")
      .withEntity(docEntity) ~> routes ~> check {
      assert(status === (StatusCodes.OK))
    }

    verify(solrStream, Mockito.times(2))
      .put(Matchers.anyString(), Matchers.any[Map[String, String]]())
  }

  test("post multiple log events to 'event' endpoint") {
    val entity = Marshal(Array(document, document)).to[MessageEntity].futureValue

    Post(uri = "/v2/events/test")
      .withEntity(entity) ~> Route.seal(routes) ~> check {
      assert(status === (StatusCodes.OK))
    }

    verify(solrStream, Mockito.times(4))
      .put(Matchers.anyString(), Matchers.any[Map[String, String]]())

  }

  test("post json array to 'json' endpoint") {
    val entity = HttpEntity(ContentTypes.`application/json`, jsonArrayDocument)

    Post(uri = "/v1/json/test")
      .withEntity(entity) ~> routes ~> check {
      assert(status === (StatusCodes.OK))
    }
  }

  test("post metric array to 'metric' endpoint") {
    val request = TimeseriesRequest("metrics", List(TimeseriesEvent(1, "key", "tag", 1.4)))
    val entity  = HttpEntity(ContentTypes.`application/json`, request.toJson.toString())

    Post(uri = "/v1/metrics")
      .withEntity(entity) ~> routes ~> check {
      assert(status === (StatusCodes.OK))
    }
  }

  test("return 400 bad request if a LogEvent isn's sent") {
    val document = Map("a" -> "b")

    val docEntity = Marshal(document).to[MessageEntity].futureValue

    Post(uri = "/log?application=test")
      .withEntity(docEntity) ~> Route.seal(routes) ~> check {
      assertResult(StatusCodes.BadRequest)(response.status)
    }
  }

  test("return endpoint not found if url string isn't included") {
    val document = new String("this shouldn't work it's expecting a LogEvent")

    val docEntity = Marshal(document).to[MessageEntity].futureValue

    Post(uri = "/log")
      .withEntity(docEntity) ~> Route.seal(routes) ~> check {
      assertResult(StatusCodes.NotFound)(response.status)
    }
  }

  test("get") {
    Get(uri = "/log") ~> routes ~> check {
      responseAs[String] == "Log Collector"
    }
  }

  test("return 500 on bad request to kudu") {
    val request = TimeseriesRequest("metrics", List(TimeseriesEvent(1, "key", "tag", 1.4)))
    val entity  = HttpEntity(ContentTypes.`application/json`, request.toJson.toString())

    when(kuduService.save(Matchers.any(), Matchers.any()))
      .thenThrow(new RuntimeException("Save failed"))

    Post(uri = "/v1/metrics")
      .withEntity(entity) ~> routes ~> check {
      val caught = intercept[RuntimeException] {
        kuduService.save(Matchers.any(), Matchers.any())
      }
      assert(caught.getMessage === "Save failed" && status === StatusCodes.InternalServerError)
    }
  }

}
