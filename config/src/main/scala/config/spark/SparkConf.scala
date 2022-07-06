package config.spark

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

class SparkConf extends TSparkConf {

  private val config: TypesafeConfig = ConfigFactory.load()

  lazy val sparkCheckpointLocation: String = config.getString("spark.checkpoint.location")

  lazy val sparkWatermark: Int = config.getInt("spark.watermark.ms")

  lazy val sparkGroupId: String = config.getString("spark.kafka.group.id")

}
