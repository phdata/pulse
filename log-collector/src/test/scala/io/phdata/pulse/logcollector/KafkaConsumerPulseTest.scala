/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import io.phdata.pulse.logcollector.util.{KafkaMiniCluster, ZooKafkaConfig}

class KafkaConsumerPulseTest extends FunSuite with BeforeAndAfterEach {
  // start kafka minicluster (broker)
  val kafkaMiniCluster = new KafkaMiniCluster(new ZooKafkaConfig)
  kafkaMiniCluster.start()


  override def beforeEach(): Unit = {

  }
  val TOPIC = "pulse_test"

  // create kafka producer
  kafkaMiniCluster.produceMessage(TOPIC)

  // create kafka consumer
  // TODO: create thread here??
  kafkaMiniCluster.consumeMessage(TOPIC)

  kafkaMiniCluster.stop()

  //TODO: how should test cases be set up?
  test("Reade messages from Kafka topic") {
// write json onto topic
    // run code and save result
    // assert result against expected
  }

  test("write to kafka topic") {
    fail()
  }

  test("read from kafka topic") {
    fail()
  }
}
