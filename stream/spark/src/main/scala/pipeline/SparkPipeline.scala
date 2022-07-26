package pipeline

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{avg, col, struct, window}
import udf.UDFs._

import javax.inject.Inject

case class SparkPipeline @Inject() () extends TSparkPipeline {

  def build(source: DataFrame): DataFrame = {
    require(source.isStreaming, "The source must be streaming.")

    import conf.SparkConf._

    def ofMilliseconds(milliSeconds: Long): String = s"$milliSeconds milliseconds"

    source
      .select(
        col("timestamp"),
        col("key").as("machine"),
        deserializeMetrics(col("value")).as("metrics"))
      .withWatermark("timestamp", ofMilliseconds(watermark))
      .groupBy(
        window(
          col("timestamp"),
          ofMilliseconds(windowSize),
          ofMilliseconds(windowStep)),
        col("machine"))
      .agg(
          avg("metrics.cpu").as("cpu"),
          avg("metrics.ram").as("ram"))
      .select(
        col("window.start").as("timestamp"),
        col("machine").as("key"),
        serializeMetrics(struct("cpu", "ram")).as("value"))
  }
}
