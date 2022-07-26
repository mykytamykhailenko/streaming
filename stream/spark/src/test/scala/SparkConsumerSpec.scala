import config.spark.TSparkConf
import config.window.TWinConf
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.Metrics
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.sql.execution.streaming.MemoryStream
import udf.UDFs._
import org.mockito.MockitoSugar.{mock, when}
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import pipeline.SparkPipeline

import java.sql.Timestamp
import java.time.Instant

class SparkConsumerSpec extends Specification with Matchers {

  "spark consumer" should {

    val spark: SparkSession =
      SparkSession.builder()
        .master("local[*]")
        .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val winConfMock = mock[TWinConf]
    when(winConfMock.windowSize).thenReturn(30000)
    when(winConfMock.windowStep).thenReturn(10000)

    val sparkConfMock = mock[TSparkConf]
    when(sparkConfMock.sparkWatermark).thenReturn(30000)

    "handle out-of-order events" in {

      implicit val sqlCtx: SQLContext = spark.sqlContext

      import spark.implicits._

      val events = Seq(
        (10L, "machine", Metrics(1, 1)),
        (50L, "machine", Metrics(5, 5)),
        (20L, "machine", Metrics(3, 3)),
        (60L, "machine", Metrics(13, 13)),
        (1000L, "watermark", Metrics(1, 1)))

      val serializedEvents = events.map { case (timestamp, machine, metrics) =>
        (Timestamp.from(Instant.ofEpochSecond(timestamp)), machine.getBytes, serialize(metrics))
      }

      val testDataStream = MemoryStream[(Timestamp, Array[Byte], Array[Byte])]

      val offset = testDataStream.addData(serializedEvents)

      val timestampedTestDataStream = testDataStream.toDF().toDF("timestamp", "key", "value")

      SparkPipeline(winConfMock, sparkConfMock)
        .build(timestampedTestDataStream)
        .writeStream
        .format("memory")
        .queryName("out")
        .outputMode("append")
        .start
        .processAllAvailable()

      testDataStream.commit(offset)

      val output =
        spark
          .sql("select * from out")
          .select(
            $"timestamp",
            $"key".as("machine"),
            deserializeMetrics($"value").as("metrics"))
          .as[(Long, String, Metrics)]
          .collect()
          .toSet

      output === Set(
        (-10, "machine", Metrics(1, 1)),
        (0, "machine", Metrics(2, 2)),
        (10, "machine", Metrics(2, 2)),
        (20, "machine", Metrics(3, 3)),
        (30, "machine", Metrics(5, 5)),
        (40, "machine", Metrics(9, 9)),
        (50, "machine", Metrics(9, 9)),
        (60, "machine", Metrics(13, 13)))
    }
  }

}
