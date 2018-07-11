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

package io.phdata.pulse.alertengine

import java.io.File
import java.util.concurrent.{ Executors, ScheduledFuture, TimeUnit }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.alertengine.notification.{
  MailNotificationService,
  NotificationServices,
  SlackNotificationService
}
import org.apache.solr.client.solrj.impl.{
  CloudSolrServer,
  HttpClientUtil,
  Krb5HttpClientConfigurer
}

import scala.io.Source
import scala.util.Try

object AlertEngineMain extends LazyLogging {
  val DAEMON_INTERVAL_MINUTES = 1
  HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer())

  def main(args: Array[String]): Unit = {
    val parsedArgs = new AlertEngineCliParser(args)
    logger.debug(s"Parsed args: $parsedArgs")

    val executorService = Executors.newSingleThreadScheduledExecutor()

    if (parsedArgs.daemonize()) {
      try {

        val scheduledFuture = executorService.scheduleAtFixedRate(new AlertEngineTask(parsedArgs),
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
      val alertTask = new AlertEngineTask(parsedArgs)
      alertTask.run()
    }

    def shutDownHook(future: ScheduledFuture[_]) = new Thread() {
      override def run(): Unit =
        try {
          logger.warn("Caught exit signal, trying to cleanup tasks")
          while (future.getDelay(TimeUnit.SECONDS) == 0) {
            logger.info("Jobs are still executing")
            Thread.sleep(1000)
          }
        } catch {
          case e: InterruptedException =>
            logger.error("Failed to clean up gracefully", e)
        }
    }
  }

  def validateConfig(config: AlertEngineConfig): Unit =
    config.applications.foreach { application =>
      assert(application.emailProfiles.isDefined || application.slackProfiles.isDefined)

    }

  def readSilencedApplications(path: String): List[String] =
    if (new File(path).exists()) {
      try {
        val silencedAlerts = Source.fromFile(path, "UTF-8")
        silencedAlerts.getLines().toList

      } catch {
        case e: Exception =>
          logger.error("Error reading silenced alerts file", e)
          List[String]()
      }
    } else {
      logger.warn(s"silenced alerts file not found at path: $path")
      List[String]()
    }

  /**
   * Task for running an alert. Can be schduled to be run repeatedly.
   * @param parsedArgs Application arguments
   */
  class AlertEngineTask(parsedArgs: AlertEngineCliParser) extends Runnable {

    override def run(): Unit = {
      logger.info("starting Alerting Engine run")
      val config = try {
        AlertEngineConfigParser.getConfig(parsedArgs.conf())
      } catch {
        case e: Exception =>
          logger.info("Error parsing configuration, exiting", e)
          System.exit(1)
          throw new RuntimeException(e) // this code won't be reached but is needed for the typechecker
      }

      logger.info(s"using config: $config")
      validateConfig(config)

      val solrServer = new CloudSolrServer(parsedArgs.zkHost())

      logger.info("starting Alert Engine run")
      val mailNotificationService =
        new MailNotificationService(parsedArgs.smtpServer(),
                                    parsedArgs.smtpPort(),
                                    parsedArgs.smtpUser(),
                                    parsedArgs.smtpPassword,
                                    parsedArgs.smtp_tls())

      val slackNotificationService = new SlackNotificationService

      val silencedApplications = parsedArgs.silencedApplicationsFile
        .map(file => readSilencedApplications(file))
        .getOrElse(List())

      val notificationFactory =
        new NotificationServices(mailNotificationService, slackNotificationService)
      try {
        val engine: AlertEngine = new AlertEngineImpl(solrServer, notificationFactory)
        engine.run(config.applications, silencedApplications)
        logger.info("ending Alert Engine run")
      } catch {
        case e: Throwable =>
          logger.error("caught exception in Collection Roller task", e)
          System.exit(1)
      } finally {
        try {
          solrServer.shutdown()
        } catch {
          case e: Exception => logger.warn("exception closing solr server", e)
        }
      }
    }
  }
}
