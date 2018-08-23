/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector.util

import java.io.File
import java.net.{InetSocketAddress, Socket}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Properties

import com.typesafe.scalalogging.LazyLogging
import kafka.server.{KafkaConfig, KafkaServerStartable}
import org.apache.commons.io.FileUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.zookeeper.server.{ServerCnxnFactory, ZooKeeperServer}
import org.apache.zookeeper.server.persistence.FileTxnSnapLog

case class ZooKafkaConfig(
    kafkaBrokerHost: String = "localhost",
    kafkaBrokerPort: Int = 11111,
    kafkaBroker: Int = 0,
    kafkaTempDir: String = "log-collector/target/embedded/kafka/" + new SimpleDateFormat(
      "yyyyMMdd'T'hhmmss").format(new Timestamp(System.currentTimeMillis())),
    zookeeperConnectionString: String = "localhost:12345",
    zookeeperDir: String = "log-collector/target/embedded/zookeeper/" + new SimpleDateFormat(
      "yyyyMMdd'T'hhmmss").format(new Timestamp(System.currentTimeMillis())),
    zookeeperPort: Int = 12345,
    zookeeperMinSessionTimeout: Int = 10000,
    zookeeperMaxSessionTimeout: Int = 30000)

class KafkaMiniCluster(config: ZooKafkaConfig) {

  private val zookeeper                       = new Zookeeper
  private val kafka                           = new Kafka
  private val services: Seq[ListeningProcess] = Seq(zookeeper, kafka)

  private val embeddedDir = new File("log-collector/target/embedded")

  def start(): Unit = {
    FileUtils.deleteDirectory(embeddedDir)
    services.foreach(_.start())
  }

  def stop(): Unit =
    services.foreach(_.stop())

  def produceMessage(topic: String, message: String): Unit = {
    // Produces single messages
    val kafkaProducerProps = new Properties()

    kafkaProducerProps.put("bootstrap.servers", "localhost:11111")
    kafkaProducerProps.put("key.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer")
    kafkaProducerProps.put("value.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](kafkaProducerProps)

    val record = new ProducerRecord[String, String](topic, message)

    println(
      "KAFKA: Producing " + record
        .value() + " to topic: " + topic + " on broker: " + config.kafkaBroker)

    producer.send(record)

    producer.close()
  }

  def produceMessages(topic: String, messages: List[String]): Unit = {
    // Produces multiple messages
    val kafkaProducerProps = new Properties()

    kafkaProducerProps.put("bootstrap.servers", "localhost:11111")
    kafkaProducerProps.put("key.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer")
    kafkaProducerProps.put("value.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](kafkaProducerProps)

    messages.foreach(msg => {
      val record = new ProducerRecord[String, String](topic, msg)

      println(
        "KAFKA: Producing " + record
          .value() + " to topic: " + topic + " on broker: " + config.kafkaBroker)

      producer.send(record)
    })

    producer.close()
  }

  class Kafka extends ListeningProcess {
    override val port: Int                        = config.kafkaBrokerPort
    override val name: String                     = "kafka"
    private var kafkaServer: KafkaServerStartable = _

    override def run(): Unit = {
      val kafkaProperties = new Properties()

      kafkaProperties.put("advertised.host.name", config.kafkaBrokerHost)
      kafkaProperties.put("port", config.kafkaBrokerPort + "")
      kafkaProperties.put("broker.id", config.kafkaBroker + "")
      kafkaProperties.put("log.dir", config.kafkaTempDir)
      kafkaProperties.put("enable.zookeeper", "true")
      kafkaProperties.put("zookeeper.connect", config.zookeeperConnectionString)

      val kafkaConf: KafkaConfig = KafkaConfig.fromProps(kafkaProperties)

      //Start local Kafka broker
      val kafkaServer = new KafkaServerStartable(kafkaConf)

      println("KAFKA: Starting Kafka on port: " + config.kafkaBrokerPort)
      kafkaServer.startup()
    }

    override def close: Unit = {
      println("Stopping Kafka...")
      kafkaServer.shutdown()
    }
  }

  class Zookeeper extends ListeningProcess {
    override val port: Int               = config.zookeeperPort
    override val name: String            = "zookeeper"
    var zooKeeperServer: ZooKeeperServer = _

    override def run(): Unit = {
      zooKeeperServer = new ZooKeeperServer()

      val snapLog = new FileTxnSnapLog(new File(config.zookeeperDir), new File(config.zookeeperDir))
      zooKeeperServer.setTxnLogFactory(snapLog)
      zooKeeperServer.setTickTime(2000)
      zooKeeperServer.setMinSessionTimeout(config.zookeeperMinSessionTimeout)
      zooKeeperServer.setMaxSessionTimeout(config.zookeeperMaxSessionTimeout)
      val cnxnFactory = ServerCnxnFactory.createFactory()
      cnxnFactory.configure(new InetSocketAddress(config.zookeeperPort), 10)
      println("Starting Zookeeper on port: " + config.zookeeperPort)
      cnxnFactory.startup(zooKeeperServer)
      cnxnFactory.join()
    }

    override def close: Unit =
      if (zooKeeperServer != null) {
        println("Stopping Zookeeper...")
        zooKeeperServer.shutdown()
      }
  }

}

trait ListeningProcess extends LazyLogging with AutoCloseable {
  val port: Int
  val name: String
  val timeoutMillis          = 30000
  private var thread: Thread = null

  def run(): Unit

  def start(): Unit = {
    var startupDurationMillis = 0
    val waitInterval          = 100
    thread = new Thread(new ProcessRunnable())
    thread.setDaemon(true)
    logger.info(s"trying to start service $name on port $port")
    thread.start()

    while (!isListening()) {
      if (startupDurationMillis > timeoutMillis) {
        throw new Exception("Process startup exceeded configured timeout")
      }

      logger.info(s"waiting for service $name")
      Thread.sleep(waitInterval)
      startupDurationMillis = startupDurationMillis + waitInterval

    }

    logger.info(s"service $name successfully started")
  }

  private def isListening(): Boolean = {
    var s: Socket = null
    try {
      s = new Socket("localhost", port)
      return true
    } catch {
      case e: Exception => false
    } finally {
      try {
        if (s != null) {
          s.close()
        }
      } catch {
        case e: Exception =>
      }
    }
  }

  def close(): Unit = {}

  def stop(): Unit = {
    try {
      close()
    } catch {
      case e: Exception => logger.info(s"Exception closing thread $name", e)
    }
    thread.interrupt()
  }

  class ProcessRunnable extends Runnable {
    override def run(): Unit = ListeningProcess.this.run()
  }
}
