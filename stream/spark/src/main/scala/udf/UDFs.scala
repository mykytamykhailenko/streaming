package udf

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.{Machine, MachineWindowed, Metrics}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.spark.sql.{Encoder, Encoders}
import org.apache.spark.sql.expressions.{Aggregator, UserDefinedFunction}
import org.apache.spark.sql.functions.{udaf, udf}

object UDFs {

  val topicless = ""

  def serialize[T](v: T)(implicit ser: Serializer[T]): Array[Byte] = ser.serialize(topicless, v)

  def deserialize[T](vs: Array[Byte])(implicit des: Deserializer[T]): T = des.deserialize(topicless, vs)

  val deserializeMachineUDF: UserDefinedFunction = udf(deserialize[Machine] _)

  val deserializeMetricsUDF: UserDefinedFunction = udf(deserialize[Metrics] _)

  val deserializeMachineWindowedUDF: UserDefinedFunction = udf(deserialize[MachineWindowed] _)

  val serializeMachineUDF: UserDefinedFunction = udf(serialize(_: Machine))

  val serializeMetricsUDF: UserDefinedFunction = udf(serialize(_: Metrics))

  val serializeMachineWindowedUDF: UserDefinedFunction = udf(serialize(_: MachineWindowed))

  object MetricsAggregator extends Aggregator[Metrics, Option[Metrics], Metrics] {
    override def zero: Option[Metrics] = None

    override def reduce(acc: Option[Metrics], metrics: Metrics): Option[Metrics] =
      Some(acc.fold(metrics)(_ + metrics))

    override def merge(left: Option[Metrics], right: Option[Metrics]): Option[Metrics] =
      left.fold(right)(l => right.fold(left)(r => Some(r + l)))

    override def finish(reduction: Option[Metrics]): Metrics = reduction.get

    override def bufferEncoder: Encoder[Option[Metrics]] = Encoders.product[Option[Metrics]]

    override def outputEncoder: Encoder[Metrics] = Encoders.product[Metrics]
  }

  val combineMetricsUDF: UserDefinedFunction = udaf(MetricsAggregator)


}
