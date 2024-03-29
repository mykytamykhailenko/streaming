import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import kafka.model.Metrics
import kafka.pipeline.KafkaPipeline
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.streams.test.TestRecord
import org.apache.kafka.streams.{TestInputTopic, TestOutputTopic, TopologyTestDriver}
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification

import java.time.Instant
import scala.jdk.CollectionConverters.{collectionAsScalaIterableConverter, seqAsJavaListConverter}

class KafkaConsumerSpec extends Specification with Matchers {

  val inputTopicName = "test-in"
  val outputTopicName = "output-topic-name"

  def getSerializerOf[T](implicit e: Serializer[T]): Serializer[T] = e

  def getDeserializerOf[T](implicit e: Deserializer[T]): Deserializer[T] = e

  def createTopics(driver: TopologyTestDriver): (TestInputTopic[String, Metrics], TestOutputTopic[String, Metrics]) = {

    val in = driver.createInputTopic(inputTopicName, getSerializerOf[String], getSerializerOf[Metrics])
    val out = driver.createOutputTopic(outputTopicName, getDeserializerOf[String], getDeserializerOf[Metrics])

    (in, out)
  }

  "kafka consumer" should {

    "handle out-of-order events (and emit records only for closed windows)" in {

      val topology = new KafkaPipeline().build(inputTopicName, outputTopicName)

      val driver = new TopologyTestDriver(topology)

      val (in, out) = createTopics(driver)

      val inputEvents = Seq(
        ("machine", Metrics(1, 1), 10L),
        ("machine", Metrics(5, 5), 50L),
        ("machine", Metrics(3, 3), 20L),
        ("machine", Metrics(13, 13), 60L),
        ("advance", Metrics(-1, -1), 1000L))
        .map { case (machine, metrics, instant) =>
          new TestRecord(machine, metrics, Instant.ofEpochMilli(instant))
        }

      in.pipeRecordList(inputEvents.asJava)

      val outputEvents = out.readRecordsToList().asScala

      outputEvents.map(testRecord => (testRecord.timestamp().longValue(), testRecord.key(), testRecord.value())) === Seq(
        (0, "machine", Metrics(2, 2)), // This window includes an out-of-order event (20L).
        (10, "machine", Metrics(2, 2)),
        (20, "machine", Metrics(3, 3)),
        (30, "machine", Metrics(5, 5)),
        (40, "machine", Metrics(9, 9)),
        (50, "machine", Metrics(9, 9)),
        (60, "machine", Metrics(13, 13))
      )
    }
  }

}
