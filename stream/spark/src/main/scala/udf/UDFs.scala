package udf

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.Metrics
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

object UDFs {

  val topicless = ""

  def serialize[T](v: T)(implicit ser: Serializer[T]): Array[Byte] = ser.serialize(topicless, v)

  def deserialize[T](vs: Array[Byte])(implicit des: Deserializer[T]): T = des.deserialize(topicless, vs)

  val deserializeMetrics: UserDefinedFunction = udf(deserialize[Metrics] _)

  val serializeMetrics: UserDefinedFunction = udf(serialize(_: Metrics))


}
