package config.producer

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

class ProducerConf extends TProducerConf {

  private val config: TypesafeConfig = ConfigFactory.load()

  lazy val machineNum: Int = config.getInt("producer.machine.num")

  lazy val speed: Int = config.getInt("producer.speed.ms")

  lazy val dispersion: Int = config.getInt("producer.dispersion.ms")

  lazy val linger: Int = config.getInt("producer.linger.ms")

  lazy val batchSize: Int = config.getInt("producer.batch.size")

  lazy val partition: Int = config.getInt("producer.partition")

}
