package flink

import com.google.inject.Guice
import flink.conf.FlinkConf
import flink.pipeline.FlinkPipeline
import flink.serde.FlinkDeserializer
import flink.serde.FlinkSerializer.{keySer, valSer}
import flink.util.Util.{EventTime, Machine, timestampAssigner, timestampRemover}
import model.Metrics
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.connector.base.DeliveryGuarantee
import org.apache.flink.connector.kafka.sink.{KafkaRecordSerializationSchema, KafkaSink}
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, createTypeInformation}

import java.time.Duration
import java.util.Properties
import javax.inject.Inject

object FlinkConsumer extends App {

  Guice
    .createInjector()
    .getInstance(classOf[FlinkConsumer])
    .execute()

}

class FlinkConsumer @Inject() () {

  import FlinkConf._

  def execute(): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(checkpoint)
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(checkpointTimeout)
    env.getCheckpointConfig.setCheckpointStorage(checkpointStorage)

    env.getConfig.setAutoWatermarkInterval(watermark)

    val kafkaSource =
      KafkaSource
        .builder[(EventTime, Machine, Metrics)]()
        .setBootstrapServers(kafkaServers)
        .setTopics(inTopic)
        .setGroupId(groupId)
        .setDeserializer(FlinkDeserializer)
        .build()

    // Good question.
    // https://stackoverflow.com/questions/58731549/flink-difference-between-maxoutoforderness-and-allowedlateness
    // https://stackoverflow.com/questions/37844871/apache-flink-how-are-late-events-handled
    // Another good question is how watermarks interact with wide dependencies.
    // The answer, the oldest watermark is selected.
    val watermarkStrategy =
      WatermarkStrategy
        .forBoundedOutOfOrderness(Duration.ofMillis(outOfOrder))
        .withIdleness(Duration.ofMillis(idle))
        .withTimestampAssigner { _ =>
          timestampAssigner
        }

    val flinkSource =
      env
        .fromSource(kafkaSource, watermarkStrategy, inTopic)
        .map(timestampRemover)

    val transactionProperty = new Properties()

    transactionProperty.put("transaction.timeout.ms", transactionTimeout)

    val sink =
      KafkaSink
        .builder[(String, Metrics)]()
        .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
        .setTransactionalIdPrefix(transactionalIdPrefix)
        .setBootstrapServers(kafkaServers)
        .setRecordSerializer(
          KafkaRecordSerializationSchema
            .builder()
            .setTopic(outTopic)
            .setKafkaKeySerializer(keySer.getClass)
            .setKafkaValueSerializer(valSer.getClass)
            .build())
        .setKafkaProducerConfig(transactionProperty)
        .build()

    // Another consideration https://stackoverflow.com/questions/63134231/which-set-checkpointing-interval-ms
    new FlinkPipeline().build(flinkSource, _.sinkTo(sink))

    env.execute()
  }

}
