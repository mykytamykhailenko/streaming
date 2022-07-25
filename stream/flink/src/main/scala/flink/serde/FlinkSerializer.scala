package flink.serde

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport.toSerializer
import model.Metrics
import org.apache.kafka.common.serialization.Serializer

object FlinkSerializer {

  // When using lambda code does not get serialized correctly.
  val keySer: Serializer[(String, Metrics)] = new Serializer[(String, Metrics)] {
    override def serialize(topic: String, data: (String, Metrics)): Array[Byte] = {
      val (machine, _) = data
      implicitly[Serializer[String]].serialize(topic, machine)
    }
  }
  val valSer: Serializer[(String, Metrics)] = new Serializer[(String, Metrics)] {
    override def serialize(topic: String, data: (String, Metrics)): Array[Byte] = {
      val (_, metrics) = data
      implicitly[Serializer[Metrics]].serialize(topic, metrics)
    }
  }

}
