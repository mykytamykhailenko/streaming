import config.flink.TFlinkConf
import config.window.TWinConf
import flink.pipeline.FlinkPipeline
import flink.util.Util.EventTime
import model.Metrics
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.mockito.MockitoSugar.{mock, when}
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

    val winConfMock = mock[TWinConf]
    when(winConfMock.windowSize).thenReturn(3)
    when(winConfMock.windowStep).thenReturn(3)

    val flinkConfMock = mock[TFlinkConf]
    when(flinkConfMock.allowedLatenessMS).thenReturn(3)

    val lag = winConfMock.windowStep - 1

    "account for out of order events" in {

      val env = StreamExecutionEnvironment.getExecutionEnvironment

      val eventSource = MockKafkaSource(Seq(
        (1L, "Machine", Metrics(1, 1)),
        (2L, "Machine", Metrics(2, 2)),
        (5L, "Machine", Metrics(5, 5)),
        (6L, "Machine", Metrics(6, 6)),
        (3L, "Machine", Metrics(4, 4)),
        (4L, "Machine", Metrics(4, 4)),
        (7L, "Machine", Metrics(7, 7))))

      val mockSource = env.addSource(eventSource)
      val mockSink = MockKafkaSink(accumulatorName = "a1")

      new FlinkPipeline(winConfMock, flinkConfMock).build(mockSource, _.addSink(mockSink))

      val job = env.execute()

      // Windows don't get closed immediately. FLink keeps their state in memory so that it can handle out-of-order events.
      // So when an out-of-order event comes, it can update the result of the window it belongs to.
      // Meaning we can observe a few duplicates, like here (3, 6).
      mockSink.getResults(job) must contain(atLeast(Set(
        (0L + lag, "Machine", Metrics(1.5, 1.5)),
        (3L + lag, "Machine", Metrics(4.5, 4.5)),
        (6L + lag, "Machine", Metrics(13, 13)) // For some reason, late firing may or may not happen (look previous commits)
      )))
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
      new FlinkPipeline(winConfMock, flinkConfMock).build(mockSource, _.addSink(mockSink))

      val job = env.execute()

      mockSink.getResults(job) must contain(atLeast(Set(
        (3L + lag, "M2", Metrics(4, 4)),
        (6L + lag, "M2", Metrics(7, 7)),
        (0L + lag, "M1", Metrics(1.5, 1.5)),
        (3L + lag, "M1", Metrics(6, 6)),
        (6L + lag, "M1", Metrics(4, 4))
      )))
    }

  }
}
