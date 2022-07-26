package flink.pipeline

import flink.conf.FlinkConf
import flink.util.Util.theOnlySample
import model.Metrics
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.extensions.acceptPartialFunctions
import org.apache.flink.streaming.api.scala.{DataStream, createTypeInformation}
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

import javax.inject.Inject

class FlinkPipeline @Inject() () extends TFlinkPipeline[(String, Metrics), (String, Metrics)] {

  import FlinkConf._

  override def build(source: DataStream[(String, Metrics)],
                     sink: DataStream[(String, Metrics)] => DataStreamSink[(String, Metrics)]): DataStreamSink[(String, Metrics)] = {

    val slidingWindow = SlidingEventTimeWindows.of(Time.milliseconds(windowSize), Time.milliseconds(windowStep))

    val pipeline =
      source
        .mapWith { case (machine, metrics) => (machine, metrics, theOnlySample) }
        .keyBy(_._1)
        .window(slidingWindow)
        .allowedLateness(Time.milliseconds(allowedLateness))
        .reduceWith { case ((machine, left, lc), (_, right, rc)) =>
          (machine, left + right, lc + rc)
        }
        .mapWith { case (machine, metrics, count) => machine -> (metrics / count) }


    sink(pipeline)
  }

}
