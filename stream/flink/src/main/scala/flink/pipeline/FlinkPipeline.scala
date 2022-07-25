package flink.pipeline

import config.flink.TFlinkConf
import config.window.TWinConf
import model.Metrics
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.extensions.acceptPartialFunctions
import org.apache.flink.streaming.api.scala.{DataStream, createTypeInformation}
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

import javax.inject.Inject

class FlinkPipeline @Inject()(winConf: TWinConf, flinkConf: TFlinkConf) extends TFlinkPipeline[(String, Metrics), (String, Metrics)] {

  override def build(source: DataStream[(String, Metrics)],
                     sink: DataStream[(String, Metrics)] => DataStreamSink[(String, Metrics)]): DataStreamSink[(String, Metrics)] = {

    val slidingWindow = SlidingEventTimeWindows.of(Time.milliseconds(winConf.windowSize), Time.milliseconds(winConf.windowStep))

    val pipeline =
      source
        .keyBy(_._1)
        .window(slidingWindow)
        .allowedLateness(Time.milliseconds(flinkConf.allowedLatenessMS))
        .reduceWith { case ((machine, left), (_, right)) =>
          machine -> (left + right)
        }

    sink(pipeline)
  }

}
