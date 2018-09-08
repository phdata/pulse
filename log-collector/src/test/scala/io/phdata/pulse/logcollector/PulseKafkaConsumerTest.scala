/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.solr.client.solrj.SolrQuery

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.phdata.pulse.logcollector.util.{KafkaMiniCluster, ZooKafkaConfig}
import io.phdata.pulse.testcommon.BaseSolrCloudTest
import io.phdata.pulse.common.{JsonSupport, SolrService}
import io.phdata.pulse.common.domain.LogEvent

class PulseKafkaConsumerTest
    extends FunSuite
    with BeforeAndAfterEach
    with JsonSupport
    with BaseSolrCloudTest {
  // start kafka minicluster (broker)
  var kafkaMiniCluster: KafkaMiniCluster = _
  var zooKafkaConfig: ZooKafkaConfig     = _

  implicit var actorSystem: ActorSystem             = _
  implicit var actorMaterializer: ActorMaterializer = _

  var solrService: SolrService            = _
  var streamProcessor: PulseKafkaConsumer = _

  override def beforeEach(): Unit = {
    zooKafkaConfig = new ZooKafkaConfig
    kafkaMiniCluster = new KafkaMiniCluster(zooKafkaConfig)
    kafkaMiniCluster.start()

    actorSystem = ActorSystem()
    actorMaterializer = ActorMaterializer.create(actorSystem)

    solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

    streamProcessor = new PulseKafkaConsumer(solrService)
  }

  override def afterEach(): Unit = {
    kafkaMiniCluster.stop()
    solrService.close()
  }

  // Set topic name and messages
  val TOPIC1 = "pulse_test"
  val TOPIC2 = "solr_test"

  def generateLogMessage ( event : LogEvent ) : String = {
    """
      |{
      | "id": """ + event.id + """
      | "category": """ + event.category + """
      | "timestamp": """ + event.timestamp + """
      | "level": """ + event.level + """
      | "message": """ + event.message + """
      | "threadName": """ + event.threadName + """
      | "throwable": """ + event.throwable + """
      | "properties": """ + event.properties + """
      | "application": """ + event.application + """
      |}
    """.stripMargin
  }

  // String JSON messages
  val document1 = LogEvent(None,
    "ERROR",
    "1970-01-01T00:00:00Z",
    "ERROR",
    "message 1",
    "thread oxb",
    Some("Exception in thread main"),
    None,
    Some("pulse-kafka-test"))

  val document2 = LogEvent(None,
    "ERROR",
    "1971-01-01T01:00:00Z",
    "INFO",
    "message 2",
    "thread oxb",
    Some("Exception in thread main"),
    None,
    Some("pulse-kafka-test"))

  val document3 = LogEvent(None,
    "ERROR",
    "1972-01-01T02:00:00Z",
    "ERROR",
    "message 3",
    "thread oxb",
    Some("Exception in thread main"),
    None,
    Some("pulse-kafka-test"))

//  val logMessage1: String = generateLogMessage(document1)
//
//  val logMessage2: String = generateLogMessage(document2)
//
//  val logMessage3: String = generateLogMessage(document3)
  val logMessage1: String =
  """
    |{
    | "id": "1",
    | "category": "cat1",
    | "timestamp": "1970-01-01T00:00:00Z",
    | "level": "ERROR",
    | "message": "Out of Bounds Exception",
    | "threadName": "thread1",
    | "throwable": "Exception in thread main",
    | "properties": {"key":"value"},
    | "application": "pulse-kafka-test"
    |}
  """.stripMargin

  val logMessage2: String =
    """
      |{
      | "id": "2",
      | "category": "cat1",
      | "timestamp": "1970-01-01T00:00:00Z",
      | "level": "INFO",
      | "message": "Null Pointer Exception",
      | "threadName": "thread2",
      | "throwable": "Exception in thread main",
      | "properties": {"key":"value"},
      | "application": "pulse-kafka-test"
      |}
    """.stripMargin

  val logMessage3: String =
    """
      |{
      | "id": "3",
      | "category": "cat2",
      | "timestamp": "1970-01-01T00:00:00Z",
      | "level": "ERROR",
      | "message": "File Not Found Exception",
      | "threadName": "thread1",
      | "throwable": "Exception in thread main",
      | "properties": {"key":"value"},
      | "application": "pulse-kafka-test"
      |}
    """.stripMargin


  test("write to solr cloud") {
    // consume messages from kafka topic
    // send to solr cloud

    // Write messages to local Kafka broker
    val messageList = List(logMessage1, logMessage2, logMessage3)
    kafkaMiniCluster.produceMessages(TOPIC2, messageList)

    // Set Kafka consumer properties
    val kafkaConsumerProps = new Properties()

    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:11111")
    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "pulse-kafka")

    val app1Name       = "pulse-kafka-test"
    val app1Collection = s"${app1Name}_1"
    val app1Alias      = s"${app1Name}_latest"

    solrService.createCollection(app1Collection, 1, 1, "testconf", null)
    solrService.createAlias(app1Alias, app1Collection)

    //  run kafka consumer in separate thread
    val f = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC2)
    }

    // sleep until documents are flushed
    Thread.sleep(6000)

    val app1Query = new SolrQuery("level: INFO")
    app1Query.set("collection", app1Alias)

    val query1Result = solrClient.query(app1Query)

    assertResult(1)(query1Result.getResults.getNumFound)
  }
}
