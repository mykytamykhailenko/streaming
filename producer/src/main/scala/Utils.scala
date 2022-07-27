import model.Metrics
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.annotation.tailrec
import scala.util.Random
import javax.inject.Inject

class Utils @Inject() () {

  import ProducerConf._

  def randomBigDecimal(): BigDecimal = BigDecimal(Random.nextInt(90) + 10)

  def randomMetrics(): Metrics = Metrics(randomBigDecimal(), randomBigDecimal())

  def randDuration() = speed + (if (dispersion != 0) Random.nextInt() % dispersion else 0)

  def getRandomMachines(): Seq[String] = {
    for {
      _ <- 1 to machines
    } yield Random.alphanumeric.take(5).mkString("")
  }

  @tailrec
  final def produceRecords(producer: KafkaProducer[String, Metrics], machine: String): Unit = {
    Thread.sleep(randDuration())
    producer.send(new ProducerRecord(kafkaTopic, machine, randomMetrics()))
    produceRecords(producer, machine)
  }

}
