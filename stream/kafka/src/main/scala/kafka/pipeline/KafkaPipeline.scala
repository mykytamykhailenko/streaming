package kafka.pipeline

import config.kafka.TKafkaConf
import config.window.TWinConf
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import kafka.util.Util.eventTimeExtractorSupplier
import model.Metrics
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded
import org.apache.kafka.streams.kstream.{Suppressed, TimeWindows}
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.StreamsBuilder

import java.time.Duration
import javax.inject.Inject

class KafkaPipeline @Inject() (winConf: TWinConf, kafkaConf: TKafkaConf) extends TKafkaPipeline {

  private val thumbingWindow = TimeWindows.ofSizeAndGrace(
    Duration.ofMillis(winConf.windowSize),
    Duration.ofMillis(kafkaConf.gracePeriod))
    .advanceBy(Duration.ofMillis(winConf.windowStep))

  def build(inputTopic: String, outputTopic: String): Topology = {

    val builder = new StreamsBuilder()

    builder.stream[String, Metrics](inputTopic)
      .groupByKey
      .windowedBy(thumbingWindow)
      .reduce(_ + _)
      .suppress(Suppressed.untilWindowCloses(unbounded()))
      .toStream
      .transform(eventTimeExtractorSupplier)
      .to(outputTopic)

    builder.build()
  }

}
