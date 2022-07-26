package kafka.pipeline

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import kafka.util.Util.eventTimeExtractorSupplier
import model.{AverageMetrics, Metrics}
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded
import org.apache.kafka.streams.kstream.{Suppressed, TimeWindows}
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.StreamsBuilder
import kafka.conf.KafkaConf

import java.time.Duration
import javax.inject.Inject

class KafkaPipeline @Inject() () extends TKafkaPipeline {

  import KafkaConf._

  private val thumbingWindow = TimeWindows.ofSizeAndGrace(
    Duration.ofMillis(windowSize),
    Duration.ofMillis(gracePeriod))
    .advanceBy(Duration.ofMillis(windowStep))

  def build(inputTopic: String, outputTopic: String): Topology = {

    val builder = new StreamsBuilder()

    builder.stream[String, Metrics](inputTopic)
      .groupByKey
      .windowedBy(thumbingWindow)
      .aggregate(AverageMetrics())((_, metrics, average) => average + metrics)
      .suppress(Suppressed.untilWindowCloses(unbounded()))
      .toStream
      .transform(eventTimeExtractorSupplier)
      .to(outputTopic)

    builder.build()
  }

}
