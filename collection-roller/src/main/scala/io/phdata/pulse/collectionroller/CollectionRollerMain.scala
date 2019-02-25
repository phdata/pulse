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

package io.phdata.pulse.collectionroller

import java.time.{ ZoneOffset, ZonedDateTime }
import java.util.concurrent.{ Executors, ScheduledFuture, TimeUnit }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.collectionroller.CollectionRollerMain.{
  createCollectionRoller,
  CollectionRollerTask
}
import io.phdata.pulse.collectionroller.util.ValidationImplicits._
import io.phdata.pulse.common.SolrService
import org.apache.solr.client.solrj.impl.CloudSolrServer

import scala.util.{ Failure, Success, Try }

object CollectionRollerMain extends LazyLogging {
  val DAEMON_INTERVAL_MINUTES       = 5L // five minutes
  val CLEANUP_SLEEP_INTERVAL_MILLIS = 100
  val executorService               = Executors.newSingleThreadScheduledExecutor()

  def main(args: Array[String]) {
    val parsedArgs = new CollectionRollerCliArgsParser(args)
    logger.debug(s"Parsed args: $parsedArgs")

    if (parsedArgs.deleteApplications.supplied) {
      deleteApplications(parsedArgs)
    } else if (parsedArgs.listApplications.supplied && parsedArgs.verbose.supplied) {
      listApplicationsVerbose(parsedArgs)
    } else if (parsedArgs.listApplications.supplied) {
      listApplications(parsedArgs)
    } else {
      val config = try {
        val c = ConfigParser.getConfig(parsedArgs.conf())
        logger.info(s"using config: $c")
        c
      } catch {
        case e: Exception =>
          logger.error("Error parsing config, exiting", e)
          System.exit(1) // bail if we have a bad config
          // this code won't be reached but is needed for the typechecker
          throw new RuntimeException("Error parsing configuration, exiting", e)
      }

      val collectionRoller = createCollectionRoller(parsedArgs.zkHosts())

      try {
        val configUploadResults =
          collectionRoller.uploadConfigsFromDirectory(config.solrConfigSetDir)

        configUploadResults match {
          case Success(_) => logger.info("Successfully uploaded config sets.")
          case Failure(e) => logger.error("Error uploading config sets", e)
        }

        if (parsedArgs.daemonize()) {
          try {
            val scheduledFuture = executorService.scheduleAtFixedRate(
              new CollectionRollerTask(collectionRoller, config, parsedArgs),
              0L,
              DAEMON_INTERVAL_MINUTES,
              TimeUnit.MINUTES)
            Runtime.getRuntime.addShutdownHook(shutDownHook(scheduledFuture))

            executorService.awaitTermination(Long.MaxValue, TimeUnit.DAYS)
          } catch {
            case e: Exception => logger.error(s"Error running CollectionRoller", e)
          } finally {
            executorService.shutdown()
          }
        } else {
          val collectionRollerTask = new CollectionRollerTask(collectionRoller, config, parsedArgs)
          collectionRollerTask.run()
        }
      }
    }

    def shutDownHook(future: ScheduledFuture[_]) = new Thread() {
      override def run(): Unit =
        try {
          logger.warn("Caught exit signal, trying to cleanup tasks")
          while (future.getDelay(TimeUnit.SECONDS) == 0) {
            logger.info("waiting for tasks to finish")
            Thread.sleep(CLEANUP_SLEEP_INTERVAL_MILLIS)
          }
        } catch {
          case e: InterruptedException =>
            logger.error("Failed to clean up gracefully", e)
        }
    }
  }

  private def listApplications(parsedArgs: CollectionRollerCliArgsParser): Unit = {
    logger.info("Starting Collection Roller List App")

    val collectionRoller = createCollectionRoller(parsedArgs.zkHosts())
    try {
      collectionRoller.collectionList().foreach(println)
    } finally {
      collectionRoller.close()
    }
  }

  private def listApplicationsVerbose(parsedArgs: CollectionRollerCliArgsParser): Unit = {
    val collectionRoller = createCollectionRoller(parsedArgs.zkHosts())
    try {
      for (app <- collectionRoller.collectionList()) {
        println(app)
        println("\t" + app + "_latest")
        for ((k, v) <- collectionRoller.aliasMap().filter(alias => alias._1 == app + "_latest"))
          println("\t\t" + v.mkString("\n"))
        println("\t" + app + "_all")
        for ((k, v) <- collectionRoller.aliasMap().filter(alias => alias._1 == app + "_all"))
          println("\t\t" + v.mkString("\n"))
      }
    } finally {
      collectionRoller.close()
    }
  }

  private def deleteApplications(parsedArgs: CollectionRollerCliArgsParser): Unit = {
    logger.info("starting Collection Roller Delete App")

    logger.debug(s"parsed applications ${parsedArgs.deleteApplications}")

    val collectionRoller = createCollectionRoller(parsedArgs.zkHosts())
    try {
      val applications =
        parsedArgs.deleteApplications.toOption.get.split(',').toList
      collectionRoller.deleteApplications(applications)
    } finally {
      collectionRoller.close()
    }
    logger.info("ending Collection Roller Delete App")
  }

  private def createCollectionRoller(zookeeper: String) = {
    val solr             = new CloudSolrServer(zookeeper)
    val solrService      = new SolrService(zookeeper, solr)
    val collectionRoller = new CollectionRoller(solrService)
    collectionRoller
  }

  class CollectionRollerTask(collectionRoller: CollectionRoller,
                             config: CollectionRollerConfig,
                             parsedArgs: CollectionRollerCliArgsParser)
      extends Runnable
      with LazyLogging {

    override def run(): Unit =
      try {
        logger.info("starting Collection Roller run")

        val now = ZonedDateTime.now(ZoneOffset.UTC)

        val collectionRollingResults =
          collectionRoller.run(config.applications, now)

        collectionRollingResults.mapInvalid(e => logger.error("fatal error", e))

        logger.info("ending Collection Roller run")
      } catch {
        case e: Exception =>
          logger.error(e.getMessage, e)
          collectionRoller.close()
          System.exit(1)
      }
  }

}
