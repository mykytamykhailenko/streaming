package util

import flink.util.Util.EventTime
import model.Metrics
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark

case class MockKafkaSource(records: Seq[(EventTime, String, Metrics)]) extends SourceFunction[(String, Metrics)] {

  def run(context: SourceFunction.SourceContext[(String, Metrics)]): Unit =
    for {
      (eventTime, machine, metrics) <- records
    } yield {
      context.collectWithTimestamp(machine -> metrics, eventTime)
      context.emitWatermark(new Watermark(eventTime)) // Without watermarks the window function stops handling out-of-order events.
    }

  def cancel(): Unit = ()

}
