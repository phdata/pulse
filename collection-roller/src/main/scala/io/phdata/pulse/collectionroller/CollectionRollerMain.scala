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
import io.phdata.pulse.common.SolrService
import org.apache.solr.client.solrj.impl.CloudSolrServer
import com.typesafe.scalalogging.LazyLogging

object CollectionRollerMain extends LazyLogging {
  val DAEMON_INTERVAL_MINUTES = 5L // five minutes
  def main(args: Array[String]) {
    val parsedArgs = new CollectionRollerCliArgsParser(args)
    logger.debug(s"Parsed args: $parsedArgs")

    val executorService = Executors.newSingleThreadScheduledExecutor()

    if (parsedArgs.daemonize()) {
      try {
        val scheduledFuture = executorService.scheduleAtFixedRate(
          new CollectionRollerTask(parsedArgs),
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
    } else if (parsedArgs.deleteApplications.supplied) {
      deleteApplications(parsedArgs)
    } else if (parsedArgs.listApplications.supplied) {
      listApplications(parsedArgs)
    } else {
      val applicationsConfig   = ConfigParser.getConfig(parsedArgs.conf())
      val collectionRollerTask = new CollectionRollerTask(parsedArgs)
      collectionRollerTask.run()
    }

    def shutDownHook(future: ScheduledFuture[_]) = new Thread() {
      override def run(): Unit =
        try {
          logger.warn("Caught exit signal, trying to cleanup tasks")
          while (future.getDelay(TimeUnit.SECONDS) == 0) {
            logger.info("waiting for tasks to finish")
            Thread.sleep(100)
          }
        } catch {
          case e: InterruptedException =>
            logger.error("Failed to clean up gracefully")
        }
    }
  }

  private def listApplications(parsedArgs: CollectionRollerCliArgsParser): Unit = {
    logger.info("Starting Collection Roller List App")

    val (solr, solrService, collectionRoller) = createServices(parsedArgs)
    try {
      collectionRoller.collectionList().foreach(println)
    } finally {
      solrService.close()
      solr.shutdown()
    }
    logger.info("Ending Collection Roller List App")
  }

  private def deleteApplications(parsedArgs: CollectionRollerCliArgsParser): Unit = {
    logger.info("starting Collection Roller Delete App")

    logger.debug(s"parsed applications ${parsedArgs.deleteApplications}")

    val (solr, solrService, collectionRoller) = createServices(parsedArgs)
    try {
      val applications =
        parsedArgs.deleteApplications.toOption.get.split(',').toList
      collectionRoller.deleteApplications(applications)
    } finally {
      solr.shutdown()
      solrService.close()
    }
    logger.info("ending Collection Roller Delete App")
  }

  private def createServices(parsedArgs: CollectionRollerCliArgsParser) = {
    val now              = ZonedDateTime.now(ZoneOffset.UTC)
    val zookeeperHosts   = parsedArgs.zkHosts()
    val solr             = new CloudSolrServer(zookeeperHosts)
    val solrService      = new SolrService(zookeeperHosts, solr)
    val collectionRoller = new CollectionRoller(solrService, now)
    (solr, solrService, collectionRoller)
  }

  class CollectionRollerTask(parsedArgs: CollectionRollerCliArgsParser)
      extends Runnable
      with LazyLogging {

    override def run(): Unit = {
      val config = try {
        ConfigParser.getConfig(parsedArgs.conf())
      } catch {
        case e: Exception => {
          logger.error("Error parsing config, exiting", e)
          System.exit(1) // bail if we have a bad config
          throw new RuntimeException("Error parsing configuration, exiting", e) // this code won't be reached but is needed for the typechecker
        }
      }
      logger.info(s"using config: $config")
      val (solr, solrService, collectionRoller) = createServices(parsedArgs)

      try {
        logger.info("starting Collection Roller run")

        config.solrConfigSetDir.foreach { dir =>
          collectionRoller.uploadConfigsFromDirectory(dir)
        }

        collectionRoller.initializeApplications(config.applications)
        collectionRoller.rollApplications(config.applications)
        logger.info("ending Collection Roller run")

      } catch {
        case t: Throwable => logger.error("Exception caught in Collection Roller task", t)
      } finally {
        try {
          solr.shutdown()
          solrService.close()
        } catch {
          case e: Exception => logger.warn("exception closing services", e)
        }
      }
    }
  }

}
