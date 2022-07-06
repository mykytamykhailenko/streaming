package pipeline

import config.kafka.TKafkaConf
import config.window.TWinConf
import model.{Machine, MachineWindowed, Metrics}
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded
import org.apache.kafka.streams.kstream.{SlidingWindows, Suppressed, TimeWindows, Windowed}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._

import java.time.Duration
import javax.inject.Inject

class KafkaPipeline @Inject() (winConf: TWinConf, kafkaConf: TKafkaConf) extends TKafkaPipeline {

  private val slidingWindow = TimeWindows.ofSizeAndGrace(
    Duration.ofMillis(winConf.windowSize),
    Duration.ofMillis(kafkaConf.gracePeriod))
    .advanceBy(Duration.ofMillis(winConf.windowStep))

  private def convertWindowedMachine(windowed: Windowed[Machine]): MachineWindowed = {
    val window = windowed.window()
    val key = windowed.key()

    MachineWindowed(
      window.start(),
      window.end(),
      key.cluster,
      key.machine)
  }

  def build(inputTopic: String, outputTopic: String): Topology = {

    val builder = new StreamsBuilder()

    builder.stream[Machine, Metrics](inputTopic)
      .groupByKey
      .windowedBy(slidingWindow)
      .reduce(_ + _)
      .suppress(Suppressed.untilWindowCloses(unbounded()))
      .toStream
      .selectKey((window, _) => convertWindowedMachine(window))
      .to(outputTopic)

    builder.build()
  }

}
