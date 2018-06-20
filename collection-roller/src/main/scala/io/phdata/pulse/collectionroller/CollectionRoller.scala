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

import java.io.{ File, FileNotFoundException }
import java.nio.file.Paths
import java.time.temporal.ChronoUnit
import java.time.{ Instant, ZoneOffset, ZonedDateTime }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService

/**
 * Collection/Alias management and rolling/rotation.
 * *
 * In Pulse Solr collection aliases are used to manage collections. Aliases point to one or more
 * collections. The collection-roller module actually does two things, it creates 'applications',
 * that is, all the collections and aliases needed to do the logging and searching we need to do,
 * and it 'rolls' application logs.
 *
 * The collection roller will first create 3 things if it detects that the application doesn't exist:
 *  - Create a collection with the application applicationName follows by a date, like 'myapp_1528381235'
 *  - Create an alias that points at the above collection called 'myapp_latest'.
 *  - Create an alias that points at the above collection called 'myapp_all'
 *
 * Since we don't want to keep logs forever, we 'roll' the application logs. To do this:
 *  - Create another collection the next day (could also be the next week) 'myapp_1528391235'
 *  - Point the 'myapp_latest' at the new collection
 *  - Point the alias 'myapp_all' at both collections.
 *
 * Continue to do the rolling every day, until we reach the max amount of collections we want to
 * keep for that application (could be a week or a month or years, depends on application
 * requirements). After we reach the max amount, we start deleting the oldest collection as we add
 * new ones.
 *
 * @param solrService
 * @param now
 */
class CollectionRoller(solrService: SolrService, val now: ZonedDateTime) extends LazyLogging {
  val DEFAULT_ROLLPERIOD      = 1
  val DEFAULT_SHARDS          = 1
  val DEFAULT_REPLICAS        = 1
  val DEFAULT_NUM_COLLECTIONS = 7
  private val nowSeconds      = now.toInstant.getEpochSecond

  /**
   * Upload Solr ConfigSetDir to Zookeeper for use in configuring Solr Collections.
   *
   * @param solrConfigSetDir Location of the ConfigSetDir
   */
  def uploadConfigsFromDirectory(solrConfigSetDir: String): Unit = {
    val directory = new File(solrConfigSetDir)

    if (!directory.exists()) {
      throw new FileNotFoundException(s"Solr config set directory not found at $solrConfigSetDir")
    }

    val solrInstanceDirectories =
      directory.listFiles.filter(_.isDirectory).toList

    logger.info(s"Attempting to upload directories ${solrInstanceDirectories.mkString(" ")}")

    solrInstanceDirectories.foreach { instanceDir =>
      try {
        solrService.uploadConfDir(Paths.get(instanceDir.getPath), instanceDir.getName)
        logger.info(s"Uploaded solr configuration from directory ${instanceDir.getAbsolutePath}")
      } catch {
        case e: Exception =>
          logger.error(s"Could not upload solr configuration: ${instanceDir.getAbsolutePath}", e)
      }
    }
  }

  /**
   * Initialize a list of applications if it doesn't exist.
   *
   * @param applications List of applications to initialize.
   */
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

  /**
   * Check if an application already exists by checking for an application alias
   *
   * @param application the [[io.phdata.pulse.collectionroller.Application]]
   * @return
   */
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

  /**
   * Initiialize application by creating
   *  - A collection for logging called ${applicationName}_{$unixTimestamp}
   *  - An alias ${applicationName}_all pointing at the above collection
   *  - An alias ${applicationName}_latest pointing at the above collection
   *
   * @param application the [[io.phdata.pulse.collectionroller.Application]] to be initialized
   * @return
   */
  private def initializeApplication(application: Application): Unit = {

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

  private def latestAliasName(applicationName: String) = s"${applicationName}_latest"

  private def getNextCollectionName(applicationName: String) =
    s"${applicationName}_$nowSeconds"

  private def searchAliasName(name: String) = s"${name}_all"

  /**
   * Roll a list of applications
   *
   * @param applications The list of [[io.phdata.pulse.collectionroller.Application]]s to be rolled
   */
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

  /**
   * Check an application should be rolled by comparing the timestamp on the newest application
   * to the configured `rollPeriod`.
   *
   * @param application
   * @return
   */
  private def shouldRollApplication(application: Application): Boolean = {
    val alias = latestAliasName(application.name)
    val collections = solrService
      .getAlias(alias)

    collections.exists { coll =>
      val collSeconds = coll.head.substring(application.name.length + 1).toLong
      val instant     = Instant.ofEpochSecond(collSeconds)
      val latestDate  = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
      ChronoUnit.DAYS.between(latestDate, now) >= application.rollPeriod.getOrElse(
        DEFAULT_ROLLPERIOD)
    }
  }

  /**
   * - Create a new collection with a newer timestamp
   * - Point the 'latest' application at this new collection
   * - Add the collection to the 'all' collection alias
   * - If needed, delete any collections older than the max collections configured
   * @param application The [[io.phdata.pulse.collectionroller.Application]] to be rolled
   * @return
   */
  private def rollCollection(application: Application): Unit = {
    logger.info(s"rolling collections for: ${application.name}")

    val nextCollection = getNextCollectionName(application.name)
    solrService.createCollection(nextCollection,
                                 application.shards.getOrElse(DEFAULT_SHARDS),
                                 application.replicas.getOrElse(DEFAULT_SHARDS),
                                 application.solrConfigSetName,
                                 null)
    deleteOldestCollection(application)

    val applicationCollections =
      solrService.listCollections().filter(_.startsWith(application.name))

    solrService.createAlias(searchAliasName(application.name), applicationCollections: _*)
    solrService.createAlias(latestAliasName(application.name), nextCollection)
  }

  /**
   * Deletes the oldest collection for an application, by timestamp.
   * @param application The [[io.phdata.pulse.collectionroller.Application]] to operate on
   */
  private def deleteOldestCollection(application: Application): Unit = {
    val appCollections =
      solrService
        .listCollections()
        .filter(_.startsWith(application.name))
    if (appCollections.lengthCompare(application.numCollections.getOrElse(DEFAULT_NUM_COLLECTIONS)) <= 0) {
      Seq()
    } else {
      val collectionToDelete = appCollections
        .sortBy { coll =>
          coll.substring(application.name.length + 1).toLong
        }
        .reverse
        .drop(appCollections.length - application.numCollections.getOrElse(DEFAULT_NUM_COLLECTIONS))

      collectionToDelete.map(coll => solrService.deleteCollection(coll))
    }
  }

  /**
   * Fully delete a list of applications including
   *  - 'latest' alias
   *  - 'all' alias
   *  - All application collections
   * @param applications [[io.phdata.pulse.collectionroller.Application]]s to delete
   */
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

  /**
   * Delete all collections belonging to an application
   * @param appName The appname to delete collections for
   */
  private def deleteCollections(appName: String): Unit = {
    val appCollections =
      solrService
        .listCollections()
        .filter(_.startsWith(appName))
    appCollections.foreach(col => solrService.deleteCollection(col))
  }

  /**
   * List all collections belonging to an application
   * @return The list of collection names
   */
  def collectionList(): List[String] =
    solrService
      .listAliases()
      .keys
      .filter(x => x.contains("_latest"))
      .map(x => x.split("_")(0))
      .toList

  /**
   * List all aliases belonging to an application
   * @return Map of alias names as keys and collection names as Set[String]
   */
  def aliasMap(): Map[String, Set[String]] =
    solrService
      .listAliases()

}
