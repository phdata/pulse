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
import io.phdata.pulse.alertengine.trigger.{ SolrAlertTrigger, SqlAlertTrigger }
import io.phdata.pulse.solr.SolrProvider

import scala.io.Source

// scalastyle:off method.length
object AlertEngineMain extends LazyLogging {
  val DAEMON_INTERVAL_MINUTES = 1

  def main(args: Array[String]): Unit = {
    val parsedArgs = new AlertEngineCliParser(args)
    logger.debug(s"Parsed args: $parsedArgs")

    val executorService = Executors.newSingleThreadScheduledExecutor()

    val mailNotificationService =
      new MailNotificationService(parsedArgs.smtpServer(),
                                  parsedArgs.smtpPort(),
                                  parsedArgs.smtpUser(),
                                  parsedArgs.smtpPassword,
                                  parsedArgs.smtp_tls())

    val slackNotificationService = new SlackNotificationService

    val notificationFactory =
      new NotificationServices(mailNotificationService, slackNotificationService)

    val config = try {
      AlertEngineConfigParser.read(parsedArgs.conf())
    } catch {
      case e: Exception =>
        logger.error("Error parsing configuration, exiting", e)
        throw e
    }
    logger.info(s"using config: $config")
    validateConfig(config)

    if (parsedArgs.daemonize()) {
      try {

        val scheduledFuture = executorService.scheduleAtFixedRate(
          new AlertEngineTask(notificationFactory, config, parsedArgs),
          0L,
          DAEMON_INTERVAL_MINUTES,
          TimeUnit.MINUTES)
        Runtime.getRuntime.addShutdownHook(shutDownHook(scheduledFuture))
        executorService.awaitTermination(Long.MaxValue, TimeUnit.DAYS)
      } catch {
        case e: Exception => logger.error(s"Error running AlertEngineMain", e)
      } finally {
        executorService.shutdown()
      }
    } else {
      val alertTask = new AlertEngineTask(notificationFactory, config, parsedArgs)
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
            Thread.currentThread().interrupt()
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
   * Task for running an alert. Can be scheduled to be run repeatedly.
   *
   * @param parsedArgs Application arguments
   */
  class AlertEngineTask(notificationFactory: NotificationServices,
                        config: AlertEngineConfig,
                        parsedArgs: AlertEngineCliParser)
      extends Runnable {

    override def run(): Unit = {
      logger.info("starting Alerting Engine run")

      val silencedApplications = parsedArgs.silencedApplicationsFile
        .map(file => readSilencedApplications(file))
        .getOrElse(List())

      val applications = config.applications

      val alertTypes = findAlertTypes(applications)

      val solrAlertTrigger = if (alertTypes.contains(AlertTypes.SOLR)) {
        Some(createSolrAlertTrigger(parsedArgs))
      } else {
        None
      }

      val sqlAlertTrigger = if (alertTypes.contains(AlertTypes.SQL)) {
        Some(createSqlAlertTrigger(parsedArgs))
      } else {
        None
      }

      try {
        val engine: AlertEngine =
          new AlertEngineImpl(solrAlertTrigger, sqlAlertTrigger, notificationFactory)
        engine.run(config.applications, silencedApplications)
        logger.info("ending Alert Engine run")
      } catch {
        case e: Throwable =>
          logger.error("caught exception in AlertEngine task", e)
      }

    }
  }

  def findAlertTypes(applications: List[Application]): Set[String] = {
    val alertTypes = applications
      .flatMap(_.alertRules)
      .flatMap(_.alertType)
      .toSet

    val unknownTypes = alertTypes -- AlertTypes.ALL_TYPES
    if (unknownTypes.nonEmpty) {
      throw new IllegalStateException(
        s"Unknown alert types found: $unknownTypes, supported types are: ${AlertTypes.ALL_TYPES}")
    }

    if (alertTypes.isEmpty) {
      Set(AlertTypes.SOLR)
    } else {
      alertTypes
    }
  }

  def createSqlAlertTrigger(parser: AlertEngineCliParser): SqlAlertTrigger = {
    val dbOptions = parser.dbOptions
      .filter(_.trim.nonEmpty)
      .map { value =>
        value
          .split(';')
          .map(_.split('='))
          .map {
            case Array(key, value)               => key -> value
            case Array(key) if !key.trim.isEmpty => key -> ""
            case unknown =>
              throw new IllegalArgumentException(
                s"Invalid database option: ${unknown.mkString("=")}")
          }
          .toMap
      }
      .getOrElse(Map.empty)

    if (parser.dbUrl.isEmpty) throw new IllegalStateException("Database URL not provided")
    new SqlAlertTrigger(parser.dbUrl(),
                        parser.dbUser.toOption.filter(_.trim.nonEmpty),
                        parser.dbPassword.toOption.filter(_.trim.nonEmpty),
                        dbOptions)
  }

  def createSolrAlertTrigger(parser: AlertEngineCliParser): SolrAlertTrigger = {
    if (parser.zkHost.isEmpty) throw new IllegalStateException("ZK host not provided")
    val solrServer = SolrProvider.create(parser.zkHost().split(",").toList)
    new SolrAlertTrigger(solrServer)
  }
}
