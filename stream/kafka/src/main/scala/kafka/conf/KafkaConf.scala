package kafka.conf

import com.typesafe.config.{Config, ConfigFactory}

object KafkaConf {

  val conf: Config = ConfigFactory.load()

  lazy val kafkaServers: String = conf.getString("kafka.servers")

  lazy val inTopic: String = conf.getString("kafka.topic.in")

  lazy val outTopic: String = conf.getString("kafka.topic.out")

  lazy val groupId: String = conf.getString("kafka.group.id")

  lazy val windowSize: Long = conf.getLong("kafka.window.size.ms")

  lazy val windowStep: Long = conf.getLong("kafka.window.step.ms")

  lazy val gracePeriod: Long = conf.getLong("kafka.grace.period.ms")

  lazy val stateDir: String = conf.getString("kafka.state.dir")

  lazy val appId: String = conf.getString("kafka.application.id")

}
