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

import java.util.concurrent.{ Executors, TimeUnit }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.{ DocumentConversion, SolrService }
import monix.eval.Task
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import monix.reactive.OverflowStrategy
import monix.reactive.OverflowStrategy.Synchronous
import monix.reactive.subjects.ConcurrentSubject

import scala.concurrent.duration._

case class SolrStreamParams(val numThreads: Int = 1,
                            val maxBufferSize: Int = 512000,
                            val batchSize: Int = 1000,
                            val batchFlushDurationSeconds: Int = 1,
                            val overflowStrategy: String = "fail")

/*
  Writes messages to Solr using some stream transformation logic.
 */
class SolrCloudStream(solrService: SolrService,
                      solrStreamParams: SolrStreamParams = SolrStreamParams())
    extends LazyLogging {

  val maxBuffersize: Int =
    sys.props
      .get("pulse.collector.stream.buffer.max")
      .map(_.toInt)
      .getOrElse(solrStreamParams.maxBufferSize)
  val batchFlushDuration = {
    val property =
      sys.props
        .get("pulse.collector.stream.flush.seconds")
        .map(_.toInt)
        .getOrElse(solrStreamParams.batchFlushDurationSeconds)

    Duration(property, TimeUnit.SECONDS)
  }
  val solrBatchSize =
    sys.props
      .get("pulse.collector.stream.batch.size")
      .map(_.toInt)
      .getOrElse(solrStreamParams.batchSize)
  val numThreads =
    sys.props
      .get("pulse.collector.stream.numthreads")
      .map(_.toInt)
      .getOrElse(solrStreamParams.numThreads)

  logger.info(s"Max buffer size `pulse.collector.stream.buffer.max` is $maxBuffersize")
  logger.info(s"Overflow stragegy `pulse.collector.stream.overflow.strategy` ${overflowStrategy}")
  logger.info(s"Batch size `pulse.collector.stream.batch.size`is $solrBatchSize")
  logger.info(s"Batch flush duration `pulse.collector.stream.flush.seconds` is $batchFlushDuration")
  logger.info(
    s"Number of threads used to publish to Solr `pulse.collector.stream.numthreads` is $numThreads")

  // This stratefy will fail by signalling a function that exits the application. The original `OverflowStrategy.Fail`
  // doesn't have a clear way get a stream failure message.
  private val failOverflowStrategy =
    OverflowStrategy.ClearBufferAndSignal(maxBuffersize, exitingOverflowMessage _)

  private val overflowStrategy =
    sys.props
      .get("pulse.collector.stream.overflow.strategy")
      .map { strategy =>
        strategy.trim.toLowerCase match {
          case "fail" =>
            failOverflowStrategy
          case "dropold" =>
            OverflowStrategy.DropOldAndSignal(maxBuffersize, overflowMessage _)
          case "dropnew" =>
            OverflowStrategy.DropNewAndSignal(maxBuffersize, overflowMessage _)
          case "clearbuffer" =>
            OverflowStrategy.ClearBufferAndSignal(maxBuffersize, overflowMessage _)
          case "backpressure" =>
            OverflowStrategy.BackPressure(maxBuffersize)
          case _ => throw new IllegalArgumentException(s"Unrecogized overflow strategy $strategy")
        }
      }
      .getOrElse {
        failOverflowStrategy
      }

  // Scheduler handles blocking http calls to Solr
  private lazy val blockingScheduler = {
    val javaService = Executors.newScheduledThreadPool(numThreads)
    Scheduler(javaService, AlwaysAsyncExecution)
  }

  // Create a subject we can write events to
  private val subject = ConcurrentSubject
    .publish[(String, Map[String, String])](overflowStrategy.asInstanceOf[Synchronous[Nothing]])

  /* Perform the transformation:
     - group the events by their application name, creating multiple streams, one for each application
     - group the events in batches of [[solrBatchSize]] or after [[solrFlushDuration]]
     - write the groups to solr
     - subscribe to the stream so every event added to the stream runs this workflow
   */
  private val transformation =
    subject
      .groupBy(x => x._1)
      .map { group =>
        group
          .bufferTimedAndCounted(batchFlushDuration, solrBatchSize)
          .mapAsync(y => Task(save(group.key, y.map(_._2))))
          .executeOn(blockingScheduler)
          .foreach(_ => ())
      }
      .subscribe()

  /**
   * Put an event onto the stream
   *
   * @param applicationName Name of the application
   * @param data            The data object, key value pairs
   */
  private[logcollector] def put(applicationName: String, data: Map[String, String]): Unit =
    subject.onNext((applicationName, data))

  /**
   * Save a batch of events to Solr
   *
   * @param appName Name of the application
   * @param events  Batch of events
   * @return An asynchronous task to be executed
   */
  private[logcollector] def save(appName: String, events: Seq[Map[String, String]]) =
    if (events.length > 0) {
      val latestCollectionAlias = s"${appName}_latest"
      logger.trace(s"Saving $latestCollectionAlias LogEvent: ${events.toString}")
      try {
        solrService.insertDocuments(latestCollectionAlias,
                                    events.map(DocumentConversion.mapToSolrDocument))
      } catch {
        case e: Exception => logger.error("Error posting documents to solr", e)
      }
    }

  private def overflowMessage[A](numEvents: Long): Option[A] = {
    logger.error(s"Dropped $numEvents messages from the queue")
    None
  }

  private[logcollector] def exitingOverflowMessage[A](numEvents: Long): Option[A] = {
    logger.error(
      "Buffer is full, failing the application",
      new RuntimeException(
        s"Failing the application because the buffer was full and 'Fail' strategy was chosen. Adjust this setting with `pulse.collector.stream.overflow.strategy`")
    )
    System.exit(1)
    None
  }

}
