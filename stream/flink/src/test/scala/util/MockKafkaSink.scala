package util

import model.{MachineWindowed, Metrics}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.accumulators.ListAccumulator
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

case class MockKafkaSink(accumulatorName: String) extends RichSinkFunction[(MachineWindowed, Metrics)] {

  override def open(parameters: Configuration): Unit = {
    getRuntimeContext.addAccumulator(accumulatorName, new ListAccumulator[(MachineWindowed, Metrics)]())
  }

  override def invoke(value: (MachineWindowed, Metrics), context: SinkFunction.Context): Unit = {
    getRuntimeContext.getAccumulator(accumulatorName).add(value)
  }

  def getResults(jobResult: JobExecutionResult): Set[(MachineWindowed, Metrics)] = {
    jobResult.getAccumulatorResult(accumulatorName).asInstanceOf[java.util.ArrayList[(MachineWindowed, Metrics)]].toSet
  }

}
