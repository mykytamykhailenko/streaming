package config.producer

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[ProducerConf])
trait TProducerConf {

  val machineNum: Int

  val speed: Int

  val dispersion: Int

  val linger: Int

  val batchSize: Int

  val partition: Int

}
