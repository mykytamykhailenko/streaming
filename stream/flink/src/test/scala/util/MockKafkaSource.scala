package util

import model.{Machine, Metrics}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark
import util.MockKafkaSource.EventTime

object MockKafkaSource {

  type EventTime = Long

}

case class MockKafkaSource(records: Seq[(EventTime, Machine, Metrics)]) extends SourceFunction[(Machine, Metrics)] {

  def run(context: SourceFunction.SourceContext[(Machine, Metrics)]): Unit =
    for {
      (eventTime, machine, metrics) <- records
    } yield {
      context.collectWithTimestamp(machine -> metrics, eventTime)
      context.emitWatermark(new Watermark(eventTime)) // Without watermarks the window function stops handling out-of-order events.
    }

  def cancel(): Unit = ()

}
