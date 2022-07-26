package flink.serde

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport.toSerializer
import model.Metrics
import org.apache.kafka.common.serialization.Serializer

object FlinkSerializer {

  // When using lambda code does not get serialized correctly.
  val keySer: Serializer[(String, Metrics)] = (topic: String, data: (String, Metrics)) => {
    val (machine, _) = data
    implicitly[Serializer[String]].serialize(topic, machine)
  }
  val valSer: Serializer[(String, Metrics)] = (topic: String, data: (String, Metrics)) => {
    val (_, metrics) = data
    implicitly[Serializer[Metrics]].serialize(topic, metrics)
  }

}
