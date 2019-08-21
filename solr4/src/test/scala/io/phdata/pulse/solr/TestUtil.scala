package io.phdata.pulse.solr

import java.io.File
import java.nio.file.Paths
import java.util.UUID

import org.apache.commons.io.FileUtils
import org.apache.solr.client.solrj.embedded.JettyConfig
import org.apache.solr.cloud.MiniSolrCloudCluster

object TestUtil {

  def miniSolrCloudCluster(): MiniSolrCloudCluster = {

    val DEFAULT_SOLR_CLOUD_XML =
      """<solr>
        |
        |  <str name="shareSchema">${shareSchema:false}</str>
        |  <str name="configSetBaseDir">${configSetBaseDir:configsets}</str>
        |  <str name="coreRootDirectory">${coreRootDirectory:target/solr4/cores}</str>
        |
        |  <shardHandlerFactory name="shardHandlerFactory" class="HttpShardHandlerFactory">
        |    <str name="urlScheme">${urlScheme:}</str>
        |    <int name="socketTimeout">${socketTimeout:90000}</int>
        |    <int name="connTimeout">${connTimeout:15000}</int>
        |  </shardHandlerFactory>
        |
        |  <solrcloud>
        |    <str name="host">127.0.0.1</str>
        |    <int name="hostPort">${hostPort:8983}</int>
        |    <str name="hostContext">${hostContext:solr}</str>
        |    <int name="zkClientTimeout">${solr.zkclienttimeout:30000}</int>
        |    <bool name="genericCoreNodeNames">${genericCoreNodeNames:true}</bool>
        |    <int name="leaderVoteWait">10000</int>
        |    <int name="distribUpdateConnTimeout">${distribUpdateConnTimeout:45000}</int>
        |    <int name="distribUpdateSoTimeout">${distribUpdateSoTimeout:340000}</int>
        |  </solrcloud>
        |
        |</solr>""".stripMargin

    System.setProperty("solr.directoryFactory", "solr.RAMDirectoryFactory")
    // clean up the solr files so we don't try to read collections from old runs
    FileUtils.deleteDirectory(new File("target/solr4"))

    // Set up a MiniSolrCloudCluster
    val clusterHome =
      s"${System.getProperty("user.dir")}/target/solr4/solrHome/${UUID.randomUUID()}"
    val jettyConfig =
      JettyConfig.builder().setContext("/solr").setPort(8983).stopAtShutdown(true).build()

    new MiniSolrCloudCluster(1, Paths.get(clusterHome), DEFAULT_SOLR_CLOUD_XML, jettyConfig)
  }

  def randomIdentifier() = UUID.randomUUID().toString.substring(0, 5)

}
