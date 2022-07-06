import config.spark.TSparkConf
import config.window.TWinConf
import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport._
import model.{Machine, MachineWindowed, Metrics}
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
        (10L, Machine("cluster", "machine"), Metrics(1, 1)),
        (50L, Machine("cluster", "machine"), Metrics(1, 1)),
        (20L, Machine("cluster", "machine"), Metrics(1, 1)),
        (60L, Machine("cluster", "machine"), Metrics(1, 1)),
        (90L, Machine("advance", "watermark"), Metrics(1, 1)))

      val serializedEvents = events.map { case (timestamp, machine, metrics) =>
        (Timestamp.from(Instant.ofEpochSecond(timestamp)), serialize(machine), serialize(metrics))
      }

      val stream = MemoryStream[(Timestamp, Array[Byte], Array[Byte])]

      val offset = stream.addData(serializedEvents)

      SparkPipeline(winConfMock, sparkConfMock)
        .build(stream.toDF().toDF("timestamp", "key", "value"))
        .writeStream
        .format("memory")
        .queryName("out")
        .outputMode("append")
        .start
        .processAllAvailable()

      stream.commit(offset)

      val output =
        spark
          .sql("select * from out")
          .select(
            deserializeMachineWindowedUDF($"key").as("machine_windowed"),
            deserializeMetricsUDF($"value").as("metrics"))
          .as[(MachineWindowed, Metrics)]
          .collect()
          .toSet

      output === Set(
        (MachineWindowed(-10, 20, "cluster", "machine"), Metrics(1, 1)),
        (MachineWindowed(0, 30, "cluster", "machine"), Metrics(2, 2)),
        (MachineWindowed(10, 40, "cluster", "machine"), Metrics(2, 2)),
        (MachineWindowed(20, 50, "cluster", "machine"), Metrics(1, 1)),
        (MachineWindowed(30, 60, "cluster", "machine"), Metrics(1, 1)))
    }
  }

}
