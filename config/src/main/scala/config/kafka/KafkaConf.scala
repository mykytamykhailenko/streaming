package config.kafka

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

class KafkaConf extends TKafkaConf {

  private val config: TypesafeConfig = ConfigFactory.load()

  val kafkaTopic: String = config.getString("kafka.topic")

  val kafkaServers: String = config.getString("kafka.servers")

  lazy val applicationId: String = config.getString("kafka.application.id")

  lazy val stateDir: String = config.getString("kafka.state.dir")

  lazy val gracePeriod: Int = config.getInt("kafka.grace.period.ms")

}
