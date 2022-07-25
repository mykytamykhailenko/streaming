package kafka.pipeline

import com.google.inject.ImplementedBy
import org.apache.kafka.streams.Topology

@ImplementedBy(classOf[KafkaPipeline])
trait TKafkaPipeline {

  def build(inputTopic: String, outputTopic: String): Topology

}
