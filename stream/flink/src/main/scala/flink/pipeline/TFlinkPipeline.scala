package flink.pipeline

import com.google.inject.ImplementedBy
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream

@ImplementedBy(classOf[FlinkPipeline])
trait TFlinkPipeline[I, O] {

  def build(source: DataStream[I], sink: DataStream[O] => DataStreamSink[O]): DataStreamSink[O]

}
