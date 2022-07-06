package config.flink

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

class FlinkConf extends TFlinkConf {

  private val config: TypesafeConfig = ConfigFactory.load()

  lazy val transactionTimeoutMS: Int = config.getInt("flink.kafka.transaction.timeout.ms")

  lazy val autoWatermarkIntervalMS: Int = config.getInt("flink.auto.watermark.interval")

  lazy val allowedLatenessMS: Int = config.getInt("flink.allowed.lateness")

  lazy val maxOutOfOrderMS: Int = config.getInt("flink.out.of.order.max.ms")

  lazy val idlenessMS: Int = config.getInt("flink.idleness.ms")

  lazy val flinkGroupId: String = config.getString("flink.kafka.group.id")

  lazy val checkpointIntervalMS: Int = config.getInt("flink.checkpoint.ms")

  lazy val checkpointTimeoutMS: Int = config.getInt("flink.checkpoint.timeout.ms")

  lazy val checkpointPauseMS: Int = config.getInt("flink.checkpoint.pause.ms")

  lazy val transactionalIdPrefix: String = config.getString("flink.transaction.id.prefix")

  lazy val checkpointStorage: String = config.getString("flink.checkpoint.storage")

  lazy val checkpointTolerableFailures: Int = config.getInt("flink.checkpoint.tolerable.failure.num")

}
