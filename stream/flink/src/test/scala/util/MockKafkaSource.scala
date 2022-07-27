package util

import flink.util.Util.{EventTime, Machine}
import model.Metrics
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark
import flink.conf.FlinkConf.outOfOrder

import scala.annotation.tailrec

case class MockKafkaSource(records: Seq[(EventTime, Machine, Metrics)]) extends SourceFunction[(Machine, Metrics)] {

  def run(context: SourceFunction.SourceContext[(Machine, Metrics)]): Unit = {

    @tailrec
    def emit(events: Seq[(EventTime, Machine, Metrics)], latestEvent: Long): Unit =
      events match {
        case (eventTime, machine, metrics) +: otherEvents =>
          val mark = Math.max(eventTime, latestEvent)

          context.collectWithTimestamp(machine -> metrics, eventTime)
          context.emitWatermark(new Watermark(mark - outOfOrder - 1))

          emit(otherEvents, mark)
        case Nil =>
      }

    val epoch = Long.MinValue + outOfOrder + 1

    emit(records, epoch)
  }

  def cancel(): Unit = ()

}
