import com.google.inject.Guice
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport.toSerializer
import model.Metrics
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.serialization.Serializer

import java.util.Properties
import java.util.concurrent.ForkJoinPool
import javax.inject.Inject

object Producer extends App {

  Guice.createInjector().getInstance(classOf[Producer]).start()

}

class Producer @Inject() (util: Utils) {

  def start(): Unit = {

    import ProducerConf._

    val props = new Properties()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers)
    props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, classOf[ProducerPartitioner])

    // Exactly one copy of each message is written in the stream.
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)

    // Makes sure the leader and its followers send acknowledgments before writing any records.
    props.put(ProducerConfig.ACKS_CONFIG, "all")

    props.put(ProducerConfig.LINGER_MS_CONFIG, linger)

    props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize)

    props.put("partition", partition)

    props.put("topic.partition", kafkaTopic)

    // How many times the producer retries the request.
    // props.put(ProducerConfig.RETRIES_CONFIG, Int.MaxValue)

    val machineSerializer = implicitly[Serializer[String]]
    val metricsSerializer = implicitly[Serializer[Metrics]]

    // Producers can be freely shared between threads.
    val producer = new KafkaProducer[String, Metrics](props, machineSerializer, metricsSerializer)

    import util._

    val machines = getRandomMachines()

    val pool = new ForkJoinPool(machines.size)

    for {
      machine <- machines
    } yield pool.execute(() => produceRecords(producer, machine))

    Runtime.getRuntime.addShutdownHook {
      new Thread(() => {
        pool.shutdown()
        producer.close()
      })
    }

    Thread.sleep(Long.MaxValue)

  }


}
