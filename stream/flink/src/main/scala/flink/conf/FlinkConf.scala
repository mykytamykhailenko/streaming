package flink.conf

import com.typesafe.config.{Config, ConfigFactory}

object FlinkConf {

  val conf: Config = ConfigFactory.load()

  lazy val kafkaServers: String = conf.getString("flink.kafka.servers")

  lazy val inTopic: String = conf.getString("flink.kafka.topic.in")

  lazy val outTopic: String = conf.getString("flink.kafka.topic.out")

  lazy val groupId: String = conf.getString("flink.kafka.group.id")

  lazy val allowedLateness: Long = conf.getLong("flink.allowed.lateness.ms")

  lazy val outOfOrder: Long = conf.getLong("flink.out.of.order.ms")

  lazy val watermark: Long = conf.getLong("flink.watermark.ms")

  lazy val idle: Long = conf.getLong("flink.idleness.ms")

  lazy val transactionalIdPrefix: String = conf.getString("flink.transaction.id.prefix")

  lazy val checkpointStorage: String = conf.getString("flink.checkpoint.storage")

  lazy val checkpoint: Long = conf.getLong("flink.checkpoint.ms")

  lazy val checkpointTimeout: Long = conf.getLong("flink.checkpoint.timeout.ms")

  lazy val checkpointPause: Long = conf.getLong("flink.checkpoint.pause.ms")

  lazy val tolerableFailures: Int = conf.getInt("flink.checkpoint.tolerable.failure.num")

  lazy val windowSize: Long = conf.getLong("flink.window.size.ms")

  lazy val windowStep: Long = conf.getLong("flink.window.step.ms")

  lazy val transactionTimeout: Long = conf.getLong("flink.kafka.transaction.timeout.ms")

}
