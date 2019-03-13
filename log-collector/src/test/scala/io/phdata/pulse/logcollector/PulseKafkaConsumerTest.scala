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

import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.scalatest.{ BeforeAndAfterEach, FunSuite }
import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.solr.client.solrj.SolrQuery

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import io.phdata.pulse.logcollector.util.{ KafkaMiniCluster, ZooKafkaConfig }
import io.phdata.pulse.testcommon.BaseSolrCloudTest
import io.phdata.pulse.common.{ JsonSupport, SolrService }
import io.phdata.pulse.common.domain.LogEvent
import spray.json._

class PulseKafkaConsumerTest
    extends FunSuite
    with BeforeAndAfterEach
    with JsonSupport
    with BaseSolrCloudTest {
  // start kafka minicluster (broker)
  var kafkaMiniCluster: KafkaMiniCluster = _
  var zooKafkaConfig: ZooKafkaConfig     = _

  var solrService: SolrService            = _
  var streamProcessor: PulseKafkaConsumer = _

  val SLEEP_TIME = 3000

  override def beforeEach(): Unit = {
    val zkPort = util.getNextPort
    zooKafkaConfig =
      ZooKafkaConfig(zookeeperPort = zkPort, zookeeperConnectionString = s"localhost:$zkPort")
    kafkaMiniCluster = new KafkaMiniCluster(zooKafkaConfig)
    kafkaMiniCluster.start()

    solrService = new SolrService(miniSolrCloudCluster.getZkServer.getZkAddress,
                                  miniSolrCloudCluster.getSolrClient)

    val solrCloudStream = new SolrCloudStream(solrService,
                                              solrStreamParams =
                                                SolrStreamParams(parallelSolrExecution = false))

    streamProcessor = new PulseKafkaConsumer(solrCloudStream)
  }

  override def afterEach(): Unit =
    kafkaMiniCluster.stop()

  // Set topic name
  val TOPIC1 = "solr_test1"
  val TOPIC2 = "solr_test2"

  // Generates random string JSON messages with given level and unique id
  def generateLogMessage(level: String, id: Integer, application: String): String = {
    val random = scala.util.Random

    LogEvent(
      id = Some(s"$id"),
      category = s"category ${Random.alphanumeric take 10 mkString}",
      timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(
        new Timestamp(System.currentTimeMillis())),
      level = level,
      message = s"message ${random.nextInt(10)}",
      threadName = s"thread ${Random.alphanumeric take 3 mkString}",
      throwable = Some(s"Exception in thread ${Random.alphanumeric take 3 mkString}"),
      properties = Some(Map(s"key${random.nextInt(10)}" -> s"value${random.nextInt(10)}")),
      application = Some(application)
    ).toJson.toString()
  }

  // Generates a balanced list of random JSON strings (half ERROR and half INFO)
  def generateMessageList(length: Integer, application: String): List[String] = {
    var i                         = 0
    var messageList: List[String] = List(generateLogMessage("ERROR", i, application))
    for (i <- 1 until length) {
      if (i % 2 == 0) {
        messageList = generateLogMessage("ERROR", i, application) :: messageList
      } else {
        messageList = generateLogMessage("INFO", i, application) :: messageList
      }
    }
    messageList
  }

  test("Produce messages, creates collection and sends messages to Solr Cloud") {
    val app1Name       = "pulse-kafka-test"
    val app1Collection = s"${app1Name}_1"
    val app1Alias      = s"${app1Name}_latest"

    // Write messages to local Kafka broker
    val messageList = generateMessageList(30, app1Name)
    kafkaMiniCluster.produceMessages(TOPIC1, messageList)

    // Set Kafka consumer properties
    val kafkaConsumerProps = new Properties()

    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                           "localhost:" + zooKafkaConfig.kafkaBrokerPort)
    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "pulse-kafka")

    solrService.createCollection(app1Collection, 1, 1, "testconf", null)
    solrService.createAlias(app1Alias, app1Collection)

    //  run kafka consumer in separate thread
    val f = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC1)
    }

    // sleep until documents are flushed
    Thread.sleep(SLEEP_TIME)

    // Query for ERROR log messages
    val app1Query = new SolrQuery("level: ERROR")
    app1Query.set("collection", app1Alias)

    val query1Result = solrClient.query(app1Query)

    assertResult(15)(query1Result.getResults.getNumFound)
  }

  test("Send two message batches to Solr Cloud") {
    val app1Name       = "pulse-kafka-test2"
    val app1Collection = s"${app1Name}_1"
    val app1Alias      = s"${app1Name}_latest"

    // Set Kafka consumer properties
    val kafkaConsumerProps = new Properties()

    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                           "localhost:" + zooKafkaConfig.kafkaBrokerPort)
    kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                           "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "pulse-kafka")

    solrService.createCollection(app1Collection, 1, 1, "testconf", null)
    solrService.createAlias(app1Alias, app1Collection)

    // run kafka consumer in separate thread for first batch
    val f1 = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC2)
    }

    Thread.sleep(1000)

    // Write first message batch to local Kafka broker
    val messageList1 = generateMessageList(30, app1Name)
    kafkaMiniCluster.produceMessages(TOPIC2, messageList1)

    // sleep until documents are flushed
    Thread.sleep(SLEEP_TIME)

    // Query for ERROR log messages
    val app1Query1 = new SolrQuery("level: ERROR")
    app1Query1.set("collection", app1Alias)

    val query1Result = solrClient.query(app1Query1)

    assertResult(15)(query1Result.getResults.getNumFound)

    // Write second message batch to local Kafka broker
    val messageList2 = generateMessageList(30, app1Name)
    kafkaMiniCluster.produceMessages(TOPIC2, messageList2)

    // run kafka consumer in separate thread for second batch
    val f2 = Future {
      streamProcessor.read(kafkaConsumerProps, TOPIC2)
    }

    // sleep until documents are flushed
    Thread.sleep(SLEEP_TIME)

    // Query for ERROR log messages
    val app1Query2 = new SolrQuery("level: ERROR")
    app1Query2.set("collection", app1Alias)

    val query2Result = solrClient.query(app1Query2)

    assertResult(15)(query2Result.getResults.getNumFound)
  }
}
