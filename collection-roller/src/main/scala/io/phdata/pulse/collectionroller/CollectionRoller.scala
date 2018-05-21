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

import java.io.File
import java.nio.file.Paths
import java.time.temporal.ChronoUnit
import java.time.{ Instant, ZoneOffset, ZonedDateTime }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService

class CollectionRoller(solrService: SolrService, val now: ZonedDateTime) extends LazyLogging {
  private val nowSeconds = now.toInstant.getEpochSecond

  val DEFAULT_ROLLPERIOD      = 1
  val DEFAULT_SHARDS          = 1
  val DEFAULT_REPLICAS        = 1
  val DEFAULT_NUM_COLLECTIONS = 7

  def uploadConfigsFromDirectory(solrConfigSetDir: String): Unit = {
    val directory = new File(solrConfigSetDir)
    val solrInstanceDirectories =
      directory.listFiles.filter(_.isDirectory).toList

    solrInstanceDirectories.foreach { instanceDir =>
      try {
        solrService.uploadConfDir(Paths.get(instanceDir.getPath), instanceDir.getName)
        logger.info(s"Uploaded solr configuration from directory ${instanceDir.getAbsolutePath}")
      } catch {
        case e: Exception =>
          logger.error(s"Could not upload solr configuration: ${instanceDir.getAbsolutePath}")
      }
    }
  }

  def initializeApplications(applications: List[Application]): Unit =
    applications.foreach { app =>
      try {
        if (!applicationExists(app)) {
          initializeApplication(app)
        }
      } catch {
        case e: Exception => logger.error(s"error initializing application ${app.name}", e)
      }
    }

  private def applicationExists(application: Application) = {
    logger.info(s"checking for application ${application.name}")
    val result = solrService.aliasExists(latestAliasName(application.name))

    if (result) {
      logger.info(s"application exists: ${application.name}")
    } else {
      logger.info(s"application does not exist: ${application.name}")
    }

    result
  }

  private def initializeApplication(application: Application) = {

    logger.info(s"creating application ${application.name}")
    val nextCollection = getNextCollectionName(application.name)
    solrService.createCollection(nextCollection,
                                 application.shards.getOrElse(DEFAULT_SHARDS),
                                 application.replicas.getOrElse(DEFAULT_REPLICAS),
                                 application.solrConfigSetName,
                                 null)

    assert(solrService.collectionExists(nextCollection),
           "collection does not exist, turn on debug logging or check solr logs")
    solrService.createAlias(latestAliasName(application.name), nextCollection)
    solrService.createAlias(searchAliasName(application.name), nextCollection)
  }

  def rollApplications(applications: List[Application]): Unit =
    applications.foreach { application =>
      try {
        if (shouldRollApplication(application)) {
          rollCollection(application)
        } else {
          logger.info(s"No actions needed for application ${application.name}")
        }
      } catch {
        case e: Exception => logger.error(s"Error rolling application ${application.name}", e)
      }
    }

  private def shouldRollApplication(app: Application): Boolean = {
    val alias = latestAliasName(app.name)
    val collections = solrService
      .getAlias(alias)

    collections.exists { coll =>
      val collSeconds = coll.head.substring(app.name.length + 1).toLong
      val instant     = Instant.ofEpochSecond(collSeconds)
      val latestDate  = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
      ChronoUnit.DAYS.between(latestDate, now) >= app.rollPeriod.getOrElse(DEFAULT_ROLLPERIOD)
    }
  }

  private def latestAliasName(name: String) = s"${name}_latest"

  private def rollCollection(app: Application) = {
    logger.info(s"rolling collections for: ${app.name}")

    val nextCollection = getNextCollectionName(app.name)
    solrService.createCollection(nextCollection,
                                 app.shards.getOrElse(DEFAULT_SHARDS),
                                 app.replicas.getOrElse(DEFAULT_SHARDS),
                                 app.solrConfigSetName,
                                 null)
    deleteOldestCollection(app)

    val applicationCollections =
      solrService.listCollections().filter(_.startsWith(app.name))

    solrService.createAlias(searchAliasName(app.name), applicationCollections: _*)
  }

  private def getNextCollectionName(applicationName: String) =
    s"${applicationName}_$nowSeconds"

  private def deleteOldestCollection(app: Application) = {
    val appCollections =
      solrService
        .listCollections()
        .filter(_.startsWith(app.name))
    if (appCollections.lengthCompare(app.numCollections.getOrElse(DEFAULT_NUM_COLLECTIONS)) <= 0) {
      Seq()
    } else {
      val collectionToDelete = appCollections
        .sortBy { coll =>
          coll.substring(app.name.length + 1).toLong
        }
        .reverse
        .drop(appCollections.length - app.numCollections.getOrElse(DEFAULT_NUM_COLLECTIONS))

      collectionToDelete.map(coll => solrService.deleteCollection(coll))
    }
  }

  private def searchAliasName(name: String) = s"${name}_all"

  def deleteApplications(applications: List[String]): Unit =
    applications.foreach { appName =>
      try {
        // deleting the 'application_latest' alias
        solrService.deleteAlias(latestAliasName(appName))

        // deleting the 'application_all' alias
        solrService.deleteAlias(searchAliasName(appName))

        // deleting all collections for the application
        deleteCollections(appName)
      } catch {
        case e: Exception => logger.error(s"Error deleting application $appName", e)
      }
    }

  private def deleteCollections(appName: String): Unit = {
    val appCollections =
      solrService
        .listCollections()
        .filter(_.startsWith(appName))
    appCollections.foreach(col => solrService.deleteCollection(col))
  }

  def collectionList(): List[String] =
    solrService
      .listAliases()
      .keys
      .filter(x => x.contains("_latest"))
      .map(x => x.split("_")(0))
      .toList

}
