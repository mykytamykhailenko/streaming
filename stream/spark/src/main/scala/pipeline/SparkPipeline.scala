package pipeline

import config.spark.TSparkConf
import config.window.TWinConf
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, struct, window}
import org.apache.spark.sql.types.LongType
import udf.UDFs._

import javax.inject.Inject

case class SparkPipeline @Inject()(winConf: TWinConf, sparkConf: TSparkConf) extends TSparkPipeline {

  def build(source: DataFrame): DataFrame = {
    require(source.isStreaming, "The source must be streaming.")

    import winConf._
    import sparkConf._

    def ofMilliseconds(milliSeconds: Long): String = s"$milliSeconds milliseconds"

    source
      .select(
        col("timestamp"),
        deserializeMachineUDF(col("key")).as("machine"),
        deserializeMetricsUDF(col("value")).as("metrics"))
      .withWatermark("timestamp", ofMilliseconds(sparkWatermark))
      .groupBy(
        window(
          col("timestamp"),
          ofMilliseconds(windowSize),
          ofMilliseconds(windowStep)),
        col("machine"))
      .agg(
        combineMetricsUDF(
          col("metrics.cpu"),
          col("metrics.ram")).alias("metrics"))
      .select(
        serializeMachineWindowedUDF(
          struct(
            col("window.start").cast(LongType).as("start"),
            col("window.end").cast(LongType).as("end"),
            col("machine.cluster").as("cluster"),
            col("machine.machine").as("machine"))).as("key"),
        serializeMetricsUDF(col("metrics")).as("value"))
  }
}
