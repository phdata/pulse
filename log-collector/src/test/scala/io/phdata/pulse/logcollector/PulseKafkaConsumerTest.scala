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

  // Generates string JSON messages
  def generateLogMessage ( eventProperties : List[String] ) : String = {
    """
      {
       "id": """" + eventProperties(0) + """",
       "category": """" + eventProperties(1) + """",
       "timestamp": """" + eventProperties(2) + """",
       "level": """" + eventProperties(3) + """",
       "message": """" + eventProperties(4) + """",
       "threadName": """" + eventProperties(5) + """",
       "throwable": """" + eventProperties(6) + """",
       "properties": """ + eventProperties(7) + """,
       "application": """" + eventProperties(8) + """"
      }
    """.stripMargin
  }

  // Define lists to pass into generateLogMessage
  val document1 = List("1",
    "ERROR",
    "1970-01-01T00:00:00Z",
    "ERROR",
    "message 1",
    "thread oxb",
    "Exception in thread main",
    "{\"key\":\"value\"}",
    "pulse-kafka-test")

  val document2 = List("2",
    "ERROR",
    "1971-01-01T01:00:00Z",
    "INFO",
    "message 2",
    "thread oxb",
    "Exception in thread main",
    "{\"key\":\"value\"}",
    "pulse-kafka-test")

  val document3 = List("3",
    "ERROR",
    "1972-01-01T02:00:00Z",
    "ERROR",
    "message 3",
    "thread oxb",
    "Exception in thread main",
    "{\"key\":\"value\"}",
    "pulse-kafka-test")

  val logMessage1: String = generateLogMessage(document1)

  val logMessage2: String = generateLogMessage(document2)

  val logMessage3: String = generateLogMessage(document3)


  test("Send two message batches to Solr Cloud") {
    // Write first message batch to local Kafka broker
    val messageList1 = List(logMessage1)
    kafkaMiniCluster.produceMessages(TOPIC2, messageList1)

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

    // run kafka consumer in separate thread for first
    val f1 = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC2)
    }

    // sleep until documents are flushed
    Thread.sleep(6000)

    val messageList2 = List(logMessage2,logMessage3)
    kafkaMiniCluster.produceMessages(TOPIC2, messageList2)

    // run kafka consumer in separate thread
    val f2 = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC2)
    }

    // sleep until documents are flushed
    Thread.sleep(6000)

    val app1Query = new SolrQuery("level: ERROR")
    app1Query.set("collection", app1Alias)

    val query1Result = solrClient.query(app1Query)

    assertResult(2)(query1Result.getResults.getNumFound)
  }

  ignore("Produce messages, creates collection and sends messages to Solr cloud") {

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
