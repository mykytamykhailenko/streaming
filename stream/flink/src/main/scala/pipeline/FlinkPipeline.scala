package pipeline

import config.flink.TFlinkConf
import config.window.TWinConf
import model.{Machine, MachineWindowed, Metrics}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.{DataStream, createTypeInformation}
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import javax.inject.Inject

class FlinkPipeline @Inject() (winConf: TWinConf, flinkConf: TFlinkConf) extends TFlinkPipeline[(Machine, Metrics), (MachineWindowed, Metrics)] {

  private object MachineMetricsAggregator extends AggregateFunction[(Machine, Metrics), Option[(Machine, Metrics)], (Machine, Metrics)] {

    def createAccumulator(): Option[(Machine, Metrics)] = None

    def add(value: (Machine, Metrics), accumulator: Option[(Machine, Metrics)]): Option[(Machine, Metrics)] =
      accumulator.fold(Some(value)) { case (machine, metrics) =>
        val (_, accumulatorMetrics) = value
        Some(machine -> (metrics + accumulatorMetrics))
      }

    def getResult(accumulator: Option[(Machine, Metrics)]): (Machine, Metrics) = accumulator.get

    def merge(left: Option[(Machine, Metrics)], right: Option[(Machine, Metrics)]): Option[(Machine, Metrics)] =
      (left, right) match {
        case (Some((machine, left)), Some((_, right))) => Some(machine -> (left + right))
        case (s@Some(_), None) => s
        case (None, s@Some(_)) => s
        case (None, None) => None
      }
  }

  private object MachineMetricsProcessWindowFunction extends ProcessWindowFunction[(Machine, Metrics), (MachineWindowed, Metrics), Machine, TimeWindow] {

    override def process(key: Machine,
                         context: Context,
                         elements: Iterable[(Machine, Metrics)],
                         out: Collector[(MachineWindowed, Metrics)]): Unit = {

      val (machine, metrics) = elements.reduce[(Machine, Metrics)] { case ((machine, l), (_, r)) =>
        machine -> (l + r)
      }

      val winStart = context.window.getStart
      val winEnd = context.window.getEnd

      out.collect((MachineWindowed(winStart, winEnd, machine.cluster, machine.machine), metrics))
    }
  }

  override def build(source: () => DataStream[(Machine, Metrics)],
                       sink: DataStream[(MachineWindowed, Metrics)] => DataStreamSink[(MachineWindowed, Metrics)]): DataStreamSink[(MachineWindowed, Metrics)] = {

    val slidingWindow = SlidingEventTimeWindows.of(Time.milliseconds(winConf.windowSize), Time.milliseconds(winConf.windowStep))

    val pipeline =
      source()
        .keyBy(_._1)
        .window(slidingWindow)
        .allowedLateness(Time.milliseconds(flinkConf.allowedLatenessMS))
        .aggregate(MachineMetricsAggregator, MachineMetricsProcessWindowFunction)

    sink(pipeline)
  }

}
