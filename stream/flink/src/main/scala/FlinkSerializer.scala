import model.{MachineWindowed, Metrics}
import org.apache.kafka.common.serialization.Serializer
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._

object FlinkSerializer {

  // When using lambda code does not get serialized correctly.
  val keySer: Serializer[(MachineWindowed, Metrics)] = new Serializer[(MachineWindowed, Metrics)] {
    override def serialize(topic: String, data: (MachineWindowed, Metrics)): Array[Byte] = {
      val (machine, _) = data
      implicitly[Serializer[MachineWindowed]].serialize(topic, machine)
    }
  }
  val valSer: Serializer[(MachineWindowed, Metrics)] = new Serializer[(MachineWindowed, Metrics)] {
    override def serialize(topic: String, data: (MachineWindowed, Metrics)): Array[Byte] = {
      val (_, metrics) = data
      implicitly[Serializer[Metrics]].serialize(topic, metrics)
    }
  }

}
