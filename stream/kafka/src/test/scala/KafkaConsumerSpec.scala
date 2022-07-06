import config.kafka.TKafkaConf
import config.window.TWinConf
import org.apache.kafka.streams.{TestInputTopic, TestOutputTopic, TopologyTestDriver}
import org.mockito.MockitoSugar.{mock, when}
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import pipeline.KafkaPipeline
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.{Machine, MachineWindowed, Metrics}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.streams.test.TestRecord

import java.time.Instant
import scala.jdk.CollectionConverters.{collectionAsScalaIterableConverter, seqAsJavaListConverter}

class KafkaConsumerSpec extends Specification with Matchers {

  val inputTopicName = "test-in"
  val outputTopicName = "output-topic-name"

  def getSerializerOf[T](implicit e: Serializer[T]): Serializer[T] = e

  def getDeserializerOf[T](implicit e: Deserializer[T]): Deserializer[T] = e

  def createTopics(driver: TopologyTestDriver): (TestInputTopic[Machine, Metrics], TestOutputTopic[MachineWindowed, Metrics]) = {

    val in = driver.createInputTopic(inputTopicName, getSerializerOf[Machine], getSerializerOf[Metrics])
    val out = driver.createOutputTopic(outputTopicName, getDeserializerOf[MachineWindowed], getDeserializerOf[Metrics])

    (in, out)
  }

  "kafka consumer" should {

    val winConfMock = mock[TWinConf]
    when(winConfMock.windowSize).thenReturn(30)
    when(winConfMock.windowStep).thenReturn(10)

    val kafkaConfMock = mock[TKafkaConf]
    when(kafkaConfMock.gracePeriod).thenReturn(30)

    "handle out-of-order events (and emit records only for closed windows)" in {

      val topology = new KafkaPipeline(winConfMock, kafkaConfMock).build(inputTopicName, outputTopicName)

      val driver = new TopologyTestDriver(topology)

      val (in, out) = createTopics(driver)

      val inputEvents = Seq(
        (Machine("cluster", "machine"), Metrics(1, 1), 10L),
        (Machine("cluster", "machine"), Metrics(1, 1), 50L),
        (Machine("cluster", "machine"), Metrics(1, 1), 20L),
        (Machine("cluster", "machine"), Metrics(1, 1), 60L),
        (Machine("advance", "advance"), Metrics(-1, -1), 1000L))
        .map { case (machine, metrics, instant) =>
          new TestRecord(machine, metrics, Instant.ofEpochMilli(instant))
        }

      in.pipeRecordList(inputEvents.asJava)

      val outputEvents = out.readRecordsToList().asScala

      outputEvents.map(testRecord => (testRecord.key(), testRecord.value())) === Seq(
        (MachineWindowed(0, 30, "cluster", "machine"), Metrics(2, 2)), // This window includes an out-of-order event (20L).
        (MachineWindowed(10, 40, "cluster", "machine"), Metrics(2, 2)),
        (MachineWindowed(20, 50, "cluster", "machine"), Metrics(1, 1)),
        (MachineWindowed(30, 60, "cluster", "machine"), Metrics(1, 1)),
        (MachineWindowed(40, 70, "cluster", "machine"), Metrics(2, 2)),
        (MachineWindowed(50, 80, "cluster", "machine"), Metrics(2, 2)),
        (MachineWindowed(60, 90, "cluster", "machine"), Metrics(1, 1))
      )
    }
  }

}
