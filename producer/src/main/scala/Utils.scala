import config.kafka.TKafkaConf
import model.Metrics
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.annotation.tailrec
import scala.util.Random
import config.producer.TProducerConf

import javax.inject.Inject

class Utils @Inject() (kafkaConf: TKafkaConf, producerConfig: TProducerConf) {

  import producerConfig._
  import kafkaConf._

  def randomBigDecimal(): BigDecimal = BigDecimal(Random.nextInt(90) + 10)

  def randomMetrics(): Metrics = Metrics(randomBigDecimal(), randomBigDecimal())

  def randDuration() = speed + (if (dispersion != 0) Random.nextInt() % dispersion else 0)

  def getRandomMachines(): Seq[String] = {
    for {
      _ <- 1 to machineNum
    } yield Random.alphanumeric.take(5).mkString("")
  }

  @tailrec
  final def produceRecords(producer: KafkaProducer[String, Metrics], machine: String): Unit = {
    Thread.sleep(randDuration())
    producer.send(new ProducerRecord(kafkaTopic, machine, randomMetrics()))
    produceRecords(producer, machine)
  }

}
