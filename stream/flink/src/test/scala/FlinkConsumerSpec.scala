import flink.conf.FlinkConf
import flink.pipeline.FlinkPipeline
import flink.util.Util.{EventTime, Machine}
import model.Metrics
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import util.{MockKafkaSink, MockKafkaSource}

class FlinkConsumerSpec extends Specification with Matchers {

  val flinkCluster = new MiniClusterWithClientResource(
    new MiniClusterResourceConfiguration.Builder()
      .setNumberSlotsPerTaskManager(2)
      .setNumberTaskManagers(1)
      .build)

  "flink consumer" should {

    val lag = FlinkConf.windowStep - 1

    "account for out of order events" in {

      val env = StreamExecutionEnvironment.getExecutionEnvironment

      // 3 and 4 are out-of-order events for allowed lateness of 3 milliseconds.
      val eventSource = MockKafkaSource(Seq(
        (1L, "machine", Metrics(1, 1)),
        (2L, "machine", Metrics(3, 3)),
        (5L, "machine", Metrics(5, 5)),
        (6L, "machine", Metrics(6, 6)),
        (3L, "machine", Metrics(4, 4)),
        (4L, "machine", Metrics(9, 9)),
        (7L, "machine", Metrics(8, 8))))

      val mockSource = env.addSource(eventSource)
      val mockSink = MockKafkaSink(accumulatorName = "a1")

      new FlinkPipeline().build(mockSource, _.addSink(mockSink))

      val job = env.execute()

      // Windows don't get closed immediately. FLink keeps their state in memory so that it can handle out-of-order events.
      // So when an out-of-order event comes, it can update the result of the window it belongs to.
      // Meaning we can observe a few duplicates.
      mockSink.getResults(job) === List[(EventTime, Machine, Metrics)](
        (0L + lag, "machine", Metrics(2, 2)),
        (3L + lag, "machine", Metrics(5, 5)),
        (3L + lag, "machine", Metrics(4.5, 4.5)),
        (3L + lag, "machine", Metrics(6, 6)),
        (6L + lag, "machine", Metrics(7, 7))
      )
    }

    "handle a few machines belonging to the same cluster" in {

      val env = StreamExecutionEnvironment.getExecutionEnvironment

      val eventSource = MockKafkaSource(Seq(
        (1L, "M1", Metrics(1, 1)),
        (2L, "M1", Metrics(2, 2)),
        (3L, "M2", Metrics(5, 5)),
        (4L, "M1", Metrics(6, 6)),
        (5L, "M2", Metrics(3, 3)),
        (6L, "M1", Metrics(4, 4)),
        (7L, "M2", Metrics(7, 7))))

      val mockSource = env.addSource(eventSource)
      val mockSink = MockKafkaSink(accumulatorName = "a2")

      // Could be created by scala-guice, but the configuration may change from test to test.
      // So it is not a good idea.
      new FlinkPipeline().build(mockSource, _.addSink(mockSink))

      val job = env.execute()

      mockSink.getResults(job) === List[(EventTime, Machine, Metrics)](
        (3L + lag, "M2", Metrics(4, 4)),
        (6L + lag, "M2", Metrics(7, 7)),
        (0L + lag, "M1", Metrics(1.5, 1.5)),
        (3L + lag, "M1", Metrics(6, 6)),
        (6L + lag, "M1", Metrics(4, 4))
      )
    }

  }
}
