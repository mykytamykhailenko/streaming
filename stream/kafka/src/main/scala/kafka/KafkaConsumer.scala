package kafka

import com.google.inject.Guice
import kafka.conf.KafkaConf
import kafka.pipeline.TKafkaPipeline
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.IsolationLevel
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

import java.util.Properties
import javax.inject.Inject

object KafkaConsumer extends App {

  Guice.createInjector().getInstance(classOf[KafkaConsumer]).start()

}

class KafkaConsumer @Inject() (pipeline: TKafkaPipeline) {

  def start(): Unit = {

    import KafkaConf._

    val props = new Properties()

    props.put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers)
    props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2)
    props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir)
    props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED)

    val topology = pipeline.build(inTopic, outTopic)

    val materializedStream = new KafkaStreams(topology, props)

    materializedStream.start()

    Thread.sleep(Long.MaxValue)

    Runtime.getRuntime.addShutdownHook {
      new Thread(() => materializedStream.close())
    }

  }
}
