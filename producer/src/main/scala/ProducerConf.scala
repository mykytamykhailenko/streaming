import com.typesafe.config.{Config, ConfigFactory}

object ProducerConf {

  val conf: Config = ConfigFactory.load()

  lazy val kafkaServers: String = conf.getString("producer.kafka.servers")

  lazy val kafkaTopic: String = conf.getString("producer.kafka.topic")

  lazy val batchSize: Int = conf.getInt("producer.batch.size")

  lazy val linger: Int = conf.getInt("producer.linger.ms")

  lazy val partition: Int = conf.getInt("producer.partition")

  lazy val machines: Int = conf.getInt("producer.machine.num")

  lazy val speed: Long = conf.getLong("producer.speed.ms")

  lazy val dispersion: Long = conf.getLong("producer.dispersion.ms")

}
