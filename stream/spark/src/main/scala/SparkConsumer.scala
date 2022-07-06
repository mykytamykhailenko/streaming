import com.google.inject.Guice
import config.kafka.TKafkaConf
import config.spark.TSparkConf
import config.window.TWinConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode
import pipeline.SparkPipeline

import javax.inject.Inject

object SparkConsumer extends App {

  Guice.createInjector().getInstance(classOf[SparkConsumer]).start()

}

class SparkConsumer @Inject() (winConf: TWinConf, sparkConf: TSparkConf, kafkaConf: TKafkaConf) {

  def start(): Unit = {
    import sparkConf._
    import kafkaConf._

    val spark =
      SparkSession.builder()
        .master("local[*]") // Should be removed for deployment.
        .config("spark.streaming.receiver.writeAheadLog.enable", "true")
        .config("spark.sql.streaming.checkpointLocation", sparkCheckpointLocation)
        .getOrCreate()

    val kafkaStream =
      spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaServers)
        .option("kafka.group.id", sparkGroupId)
        .option("subscribe", kafkaTopic)
        .load()

    SparkPipeline(winConf, sparkConf).build(kafkaStream)
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServers)
      .option("topic", s"spark-total-$kafkaTopic")
      .outputMode(OutputMode.Append()) // Does not create any output until the window is closed.
      .start()
      .awaitTermination()

  }

}
