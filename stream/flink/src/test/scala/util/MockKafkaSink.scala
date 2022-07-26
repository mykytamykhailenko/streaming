package util

import flink.util.Util.EventTime
import model.Metrics
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.accumulators.ListAccumulator
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}

import java.util
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

case class MockKafkaSink(accumulatorName: String) extends RichSinkFunction[(String, Metrics)] {

  override def open(parameters: Configuration): Unit = {
    getRuntimeContext.addAccumulator(accumulatorName, new ListAccumulator[(EventTime, String, Metrics)]())
  }

  override def invoke(value: (String, Metrics), context: SinkFunction.Context): Unit = {
    val (machine, metrics) = value
    getRuntimeContext.getAccumulator(accumulatorName).add((context.timestamp(), machine, metrics))
  }

  def getResults(jobResult: JobExecutionResult): List[(EventTime, String, Metrics)] = {
    jobResult.getAccumulatorResult(accumulatorName).asInstanceOf[util.ArrayList[(EventTime, String, Metrics)]].toList
  }

}
