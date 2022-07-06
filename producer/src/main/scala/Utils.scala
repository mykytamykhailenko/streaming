import config.kafka.TKafkaConf
import model.{Machine, Metrics}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.annotation.tailrec
import scala.util.Random
import config.producer.TProducerConf

import javax.inject.Inject

class Utils @Inject() (kafkaConf: TKafkaConf, producerConfig: TProducerConf) {

  import producerConfig._
  import kafkaConf._

  def randomName(): String = Random.alphanumeric.take(5).mkString("")

  def randomBigDecimal(): BigDecimal = BigDecimal(Random.nextInt(90) + 10)

  def randomMetrics(): Metrics = Metrics(randomBigDecimal(), randomBigDecimal())

  def randDuration() = speed + (if (dispersion != 0) Random.nextInt() % dispersion else 0)

  def getRandomMachines(): Seq[Machine] = {

    val clusterNames = for {
      _ <- 1 to clusterNum
    } yield randomName()

    for {
      cluster <- clusterNames
      _ <- 1 to machineNum
    } yield Machine(cluster, randomName())
  }

  @tailrec
  final def produceRecords(producer: KafkaProducer[Machine, Metrics], machine: Machine): Unit = {
    Thread.sleep(randDuration())
    producer.send(new ProducerRecord(kafkaTopic, machine, randomMetrics()))
    produceRecords(producer, machine)
  }

}
