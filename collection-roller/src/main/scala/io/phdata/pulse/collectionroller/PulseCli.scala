/* Copyright 2018 phData Inc. */

package io.phdata.pulse.collectionroller

import java.time.{ ZoneOffset, ZonedDateTime }

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pulse.common.SolrService
import org.apache.solr.client.solrj.impl.CloudSolrServer
import org.rogach.scallop.{ ScallopConf, Subcommand }

class PulseCli extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val config = new Config(args)

    config.subcommands match {
      case config.applications => {
        config.applications.subcommands match {
          case config.applications.config =>
            throw new NotImplementedError()
        }

        if (config.applications.list.isDefined) {
          val collectionRoller = createCollectionRoller(config.applications.config.zookeeper())
          collectionRoller.collectionList().foreach(println)
        }

        if (config.applications.delete.isDefined) {
          val collectionRoller = createCollectionRoller(config.applications.config.zookeeper())
          collectionRoller.deleteApplications(config.applications.delete())
        }
      }

      case config.alerts => throw new NotImplementedError()
    }
  }

  private def createCollectionRoller(zookeeperHost: String): CollectionRoller = {
    val now              = ZonedDateTime.now(ZoneOffset.UTC)
    val solr             = new CloudSolrServer(zookeeperHost)
    val solrService      = new SolrService(zookeeperHost, solr)
    val collectionRoller = new CollectionRoller(solrService, now)
    collectionRoller
  }
}

class Config(args: Seq[String]) extends ScallopConf(args) {
  val alerts = new Subcommand("alerts") {
    val config = new Subcommand("config") {
      val upload    = opt[String]("upload")
      val validate  = opt[String]("validate")
      val file      = opt[String]("file")
      val zookeeper = opt[String]("zookeeper")
    }
    val list      = opt[String]("list")
    val zookeeper = opt[String]("zookeeper")

    addSubcommand(config)
  }

  val applications = new Subcommand("applications") {
    val config = new Subcommand("config") {
      val upload    = opt[String]("upload")
      val validate  = opt[String]("validate")
      val file      = opt[String]("file")
      val show      = opt[String]("show")
      val zookeeper = opt[String]("zookeeper")
    }

    val delete    = opt[List[String]]("delete")
    val list      = opt[String]("list")
    val verbose   = opt[String]("verbose")
    val zookeeper = opt[String]("zookeeper")

    addSubcommand(config)
  }

  addSubcommand(alerts)
  verify()
}
