package config.spark

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[SparkConf])
trait TSparkConf {

  val sparkCheckpointLocation: String

  val sparkWatermark: Int

  val sparkGroupId: String

}
