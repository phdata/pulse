/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import java.util.{ Collections, Properties }

import akka.actor.{ ActorRef, ActorSystem }
import org.apache.kafka.clients.consumer.{ ConsumerRecords, KafkaConsumer }
import io.phdata.pulse.common.domain.LogEvent
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.{ JsonSupport, SolrService }

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import spray.json._

/**
 * Reads json from a given topic
 * parses into a case class
 * returns the value
 * @param solrService
 */
class KafkaConsumerPulse(solrService: SolrService) extends JsonSupport with LazyLogging {
  implicit val solrActorSystem: ActorSystem             = ActorSystem()
  implicit val solrActorMaterializer: ActorMaterializer = ActorMaterializer.create(solrActorSystem)

  val solrInputStream: ActorRef = new SolrCloudStreams(solrService).groupedInsert.run()

  def read(consumerProperties: Properties, topic: String): Unit = {
    val consumer = new KafkaConsumer[String, String](consumerProperties)

    consumer.subscribe(Collections.singletonList(topic))

    var recordList = new ListBuffer[String]

    while (true) {
      val records: ConsumerRecords[String, String] = consumer.poll(100)
      for (record <- records.asScala) {
        println("KAFKA: Consuming " + record.value() + " from topic: " + topic)
        recordList += record.value()
        solrInputStream ! (record
          .value()
          .parseJson
          .convertTo[LogEvent]
          .application, record
          .value()
          .parseJson
          .convertTo[LogEvent]) // write messages onto our solr stream
      }
    }
  }

  def consumeMessage(consumerProperties: Properties, topic: String): LogEvent = {

    val consumer = new KafkaConsumer[String, String](consumerProperties)

    consumer.subscribe(Collections.singletonList(topic))

    var recordList = new ListBuffer[String]

    while (recordList.isEmpty) {
      val records: ConsumerRecords[String, String] = consumer.poll(100)
      for (record <- records.asScala) {
        println("KAFKA: Consuming " + record.value() + " from topic " + topic)
        recordList += record.value()
      }
    }
    recordList.head.parseJson.convertTo[LogEvent]
  }

//  def read() =
//    while (true /* read messages from kafka */ ) {
//      solrInputStream ! ("app name", LogEvent) // write messages onto our solr stream
//    }

  /* TODO:
   1) start kafka mini cluster
   2) write JSON messages to kafka broker
   3) read JSON messages and parse into case classes
   4) send messages to solr (read over solrcloudstreams class and tests) and sleep
   5) assert message received in solr (test cases already made for this)
 */
}
