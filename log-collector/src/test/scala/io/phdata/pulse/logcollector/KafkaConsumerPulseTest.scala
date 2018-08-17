/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import java.nio.file.Paths
import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.phdata.pulse.common.{ JsonSupport, SolrService }
import io.phdata.pulse.common.domain.LogEvent
import org.scalatest.{ BeforeAndAfterEach, FunSuite }
import io.phdata.pulse.logcollector.util.{ KafkaMiniCluster, ZooKafkaConfig }
import io.phdata.pulse.testcommon.BaseSolrCloudTest
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.apache.solr.cloud.MiniSolrCloudCluster
import spray.json._

class KafkaConsumerPulseTest
    extends FunSuite
    with BeforeAndAfterEach
    with JsonSupport
    with BaseSolrCloudTest {
  // start kafka minicluster (broker)
  var kafkaMiniCluster: KafkaMiniCluster = _
  var zooKafkaConfig: ZooKafkaConfig     = _

  implicit var actorSystem: ActorSystem             = _
  implicit var actorMaterializer: ActorMaterializer = _

  var solrService: SolrService          = _
  var streamProcessor: SolrCloudStreams = _

  override def beforeEach(): Unit = {
    zooKafkaConfig = new ZooKafkaConfig
    kafkaMiniCluster = new KafkaMiniCluster(zooKafkaConfig)
    kafkaMiniCluster.start()

    actorSystem = ActorSystem()
    actorMaterializer = ActorMaterializer.create(actorSystem)

    solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress, solrClient)

    streamProcessor = new SolrCloudStreams(solrService)
  }

  override def afterEach(): Unit = {
    kafkaMiniCluster.stop()
    solrService.close()
  }

  // Set topic name and messages
  val TOPIC1 = "pulse_test"
  val TOPIC2 = "solr_test"

  // String JSON messages
  val logMessage1: String =
    """
      |{
      | "id": "1",
      | "category": "cat1",
      | "timestamp": "8/7/18",
      | "level": "ERROR",
      | "message": "Out of Bounds Exception",
      | "threadName": "thread1",
      | "throwable": "Exception in thread main",
      | "properties": {"key":"value"}
      |}
    """.stripMargin

  val logMessage2: String =
    """
      |{
      | "id": "2",
      | "category": "cat1",
      | "timestamp": "8/13/18",
      | "level": "ERROR",
      | "message": "Null Pointer Exception",
      | "threadName": "thread2",
      | "throwable": "Exception in thread main",
      | "properties": {"key":"value"}
      |}
    """.stripMargin

  val logMessage3: String =
    """
      |{
      | "id": "3",
      | "category": "cat2",
      | "timestamp": "8/20/18",
      | "level": "ERROR",
      | "message": "File Not Found Exception",
      | "threadName": "thread1",
      | "throwable": "Exception in thread main",
      | "properties": {"key":"value"}
      |}
    """.stripMargin

  // LogEvent messages
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
                               "1971-01-01T01:00:00Z",
                               "ERROR",
                               "message 2",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

  val document3 = new LogEvent(None,
                               "ERROR",
                               "1972-01-01T02:00:00Z",
                               "ERROR",
                               "message 3",
                               "thread oxb",
                               Some("Exception in thread main"),
                               None)

//  test("read messages from Kafka topic") {
//    // write json onto topic
//    // run code and save result
//    // assert result against expected
//
//    // Write messages to local Kafka broker
//    kafkaMiniCluster.produceMessage(TOPIC1, logMessage1)
//
//    // Set Kafka consumer properties
//    val kafkaConsumerProps = new Properties()
//
//    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:11111")
//    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//      "org.apache.kafka.common.serialization.StringDeserializer")
//    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//      "org.apache.kafka.common.serialization.StringDeserializer")
//    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")
//    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,"pulse-kafka")
//
//    val kafkaConsumerPulse = new KafkaConsumerPulse(solrService)
//
//    val consumedMessage: LogEvent = kafkaConsumerPulse.consumeMessage(kafkaConsumerProps,TOPIC1)
//
//    val logMessageCC: LogEvent = logMessage1.parseJson.convertTo[LogEvent]
//
//    println("expected: " + logMessageCC)
//    println("actual:   " + consumedMessage)
//
//    // Check whether consumed message equals expected message
//    assert( logMessageCC == consumedMessage )
//  }

  test("write to solr cloud") {
    // consume messages from kafka topic
    // send to solr cloud

    // Write messages to local Kafka broker
    //kafkaMiniCluster.produceMessage(TOPIC2, logMessage1)

    // Set Kafka consumer properties
//    val kafkaConsumerProps = new Properties()
//
//    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:11111")
//    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//      "org.apache.kafka.common.serialization.StringDeserializer")
//    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//      "org.apache.kafka.common.serialization.StringDeserializer")
//    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")
//    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,"pulse-kafka")

    val app1Name       = "pulse-kafka-test"
    val app1Collection = s"${app1Name}_1"
    val app1Alias      = s"${app1Name}_latest"

    println("Setting up Solr collection")
    solrService.createCollection(app1Collection, 1, 1, "testconf", null)
    solrService.createAlias(app1Alias, app1Collection)
    //streamProcessor.read(kafkaConsumerProps,TOPIC2)
    println("Sending to solr")

    val solrStream = streamProcessor.groupedInsert.run()

    solrStream ! (app1Name, document1)

    println("Finished consuming")
    Thread.sleep(streamProcessor.GROUP_MAX_TIME.toMillis + 5000)

    val app1Query = new SolrQuery("level: ERROR")
    app1Query.set("collection", app1Alias)
    println("Setting up SolrQuery")

    val query1Result = solrClient.query(app1Query)
    println("Setting up query result")

    assertResult(1)(query1Result.getResults.getNumFound)
  }

//  test("read from kafka topic") {
//    fail()
//  }
}
