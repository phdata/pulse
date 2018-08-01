/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import org.scalatest.FunSuite
import java.util.{Collections,Properties}
import java.io.File

import kafka.server.KafkaConfig
import kafka.server.KafkaServerStartable
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.consumer._

import scala.collection.JavaConverters._


import org.apache.curator.test.{InstanceSpec, TestingServer}
import org.apache.zookeeper.server.ServerConfig
import org.apache.zookeeper.server.ZooKeeperServerMain
import org.apache.zookeeper.server.quorum.QuorumPeerConfig

class KafkaConsumerPulseTest extends FunSuite {
  // start kafka minicluster (broker)

  //Zookeeper configurations
  val fileDir = new File("embedded_zookeeper")
  val port = 12345
  val electionPort = 20001
  val quorumPort = 20002
  val deleteDataDirectoryOnClose = false
  val serverId = 1
  val tickTime = 2000
  val maxClientCnxns = 60

  //Start local Zookeeper
  //  val quorumConfiguration = new QuorumPeerConfig()
  //  quorumConfiguration.parseProperties(zookeeperProperties)
  //
  //  val zooKeeperServer = new ZooKeeperServerMain()
  //
  //  val configuration = new ServerConfig()
  //
  //  configuration.readFrom(quorumConfiguration)
  //  zooKeeperServer.runFromConfig(configuration)

  System.out.print("ZOOKEEPER: Starting Zookeeper on port: " + port)
  val spec = new InstanceSpec(fileDir, port, electionPort, quorumPort, deleteDataDirectoryOnClose, serverId, tickTime, maxClientCnxns)
  val testingServer = new TestingServer(spec, true)

  //Kafka configurations
  val kafkaHostname = "localhost"
  val kafkaPort = 11111
  val kafkaBrokerId = 0
  val kafkaTempDir = "embedded_kafka"
  val zookeeperConnectionString = "localhost:12345"


  //Store all configurations in a properties type
  val kafkaProperties = new Properties()

  kafkaProperties.put("advertised.host.name", kafkaHostname)
  kafkaProperties.put("port", kafkaPort + "")
  kafkaProperties.put("broker.id", kafkaBrokerId + "")
  kafkaProperties.put("log.dir", kafkaTempDir)
  kafkaProperties.put("enable.zookeeper", "true")
  kafkaProperties.put("zookeeper.connect", zookeeperConnectionString)

  val kafkaConfig: KafkaConfig = KafkaConfig.fromProps(kafkaProperties)

  //Start local Kafka broker
  val kafka = new KafkaServerStartable(kafkaConfig)

  val waitInt = 5000
  System.out.print("Waiting...")
  Thread.sleep(waitInt)
  System.out.print("KAFKA: Starting Kafka on port: " + kafkaPort)
  kafka.startup()

  // create kafka producer
  val kafkaProducerProps = new Properties()

  kafkaProducerProps.put("bootstrap.servers", "localhost:11111")
  kafkaProducerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  kafkaProducerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](kafkaProducerProps)

  val TOPIC="pulse_test"

  for(i<- 1 to 50){
    val record = new ProducerRecord(TOPIC, "key", s"sam $i")
    producer.send(record)
  }

  val record = new ProducerRecord(TOPIC, "key", "the end " + new java.util.Date)
  producer.send(record)

  producer.close()

  // create kafka consumer
  val  kafkaConsumerProps = new Properties()

  kafkaConsumerProps.put("bootstrap.servers", "localhost:11111")
  kafkaConsumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  kafkaConsumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  kafkaConsumerProps.put("group.id", "something")

  val consumer = new KafkaConsumer[String, String](kafkaConsumerProps)

  consumer.subscribe(Collections.singletonList(TOPIC))

  while(true){
    val records=consumer.poll(100)
    for (record<-records.asScala){
      println(record)
    }
  }

  test("create kafka topic") {
    fail()
  }

  test("write to kafka topic") {
    fail()
  }

  test("read from kafka topic") {
    fail()
  }
}
