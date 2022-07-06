package config.flink

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[FlinkConf])
trait TFlinkConf {

  val transactionTimeoutMS: Int

  val autoWatermarkIntervalMS: Int

  val allowedLatenessMS: Int

  val maxOutOfOrderMS: Int

  val idlenessMS: Int

  val flinkGroupId: String

  val checkpointIntervalMS: Int

  val checkpointTimeoutMS: Int

  val checkpointPauseMS: Int

  val transactionalIdPrefix: String

  val checkpointStorage: String

  val checkpointTolerableFailures: Int

}
