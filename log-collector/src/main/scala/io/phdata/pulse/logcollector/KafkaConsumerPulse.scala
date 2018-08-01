/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import akka.actor.ActorSystem
import akka.event.Logging.LogEvent
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.{ JsonSupport, SolrService }

class KafkaConsumerPulse(solrService: SolrService) extends JsonSupport with LazyLogging {
  implicit val solrActorSystem: ActorSystem = ActorSystem()
  implicit val solrActorMaterializer        = ActorMaterializer.create(solrActorSystem)

  val solrInputStream = new SolrCloudStreams(solrService).groupedInsert.run()

  def read() =
    while (true /* read messages from kafka */ ) {
      solrInputStream ! ("app name", LogEvent) // write messages onto our solr stream
    }
}
