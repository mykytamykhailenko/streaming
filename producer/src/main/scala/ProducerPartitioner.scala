import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster

import java.util.{Map => JavaMap}
import scala.jdk.CollectionConverters.mapAsScalaMapConverter

class ProducerPartitioner extends Partitioner {

  var partition: Int = _
  var kafkaTopic: String = _

  def partition(topic: String,
                key: Any,
                keyBytes: Array[Byte],
                value: Any,
                valueBytes: Array[Byte],
                cluster: Cluster): Int = {

    val partitions = cluster.partitionsForTopic(kafkaTopic).size()

    partition % partitions
  }

  def close(): Unit = ()

  def configure(configs: JavaMap[String, _]): Unit = {
    configs.asScala.get("partition").foreach { p =>
      partition = p.asInstanceOf[Int]
    }
    configs.asScala.get("topic.partition").foreach { p =>
      kafkaTopic = p.asInstanceOf[String]
    }
  }

}
