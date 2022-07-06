package config.kafka

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[KafkaConf])
trait TKafkaConf {

  val kafkaTopic: String

  val kafkaServers: String

  val applicationId: String

  val stateDir: String

  val gracePeriod: Int

}
