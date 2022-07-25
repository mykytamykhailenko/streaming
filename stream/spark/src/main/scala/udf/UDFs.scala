package udf

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.Metrics
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.spark.sql.{Encoder, Encoders}
import org.apache.spark.sql.expressions.{Aggregator, UserDefinedFunction}
import org.apache.spark.sql.functions.{udaf, udf}

object UDFs {

  val topicless = ""

  def serialize[T](v: T)(implicit ser: Serializer[T]): Array[Byte] = ser.serialize(topicless, v)

  def deserialize[T](vs: Array[Byte])(implicit des: Deserializer[T]): T = des.deserialize(topicless, vs)

  val deserializeMetrics: UserDefinedFunction = udf(deserialize[Metrics] _)

  val serializeMetrics: UserDefinedFunction = udf(serialize(_: Metrics))

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

  val combineMetrics: UserDefinedFunction = udaf(MetricsAggregator)


}
