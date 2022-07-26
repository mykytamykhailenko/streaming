package conf

import com.typesafe.config.{Config, ConfigFactory}

object SparkConf {

  val conf: Config = ConfigFactory.load()

  lazy val kafkaServers: String = conf.getString("spark.kafka.servers")

  lazy val inTopic: String = conf.getString("spark.kafka.topic.in")

  lazy val outTopic: String = conf.getString("spark.kafka.topic.out")

  lazy val groupId: String = conf.getString("spark.kafka.group.id")

  lazy val checkpointLocation: String = conf.getString("spark.checkpoint.location")

  lazy val watermark: Long = conf.getLong("spark.watermark.ms")

  lazy val windowSize: Long = conf.getLong("spark.window.size.ms")

  lazy val windowStep: Long = conf.getLong("spark.window.step.ms")

}
