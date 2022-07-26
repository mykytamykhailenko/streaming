import com.google.inject.Guice
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode
import pipeline.SparkPipeline

import javax.inject.Inject

object SparkConsumer extends App {

  Guice.createInjector().getInstance(classOf[SparkConsumer]).start()

}

class SparkConsumer @Inject() () {

  def start(): Unit = {
    import conf.SparkConf._

    val spark =
      SparkSession.builder()
        .master("local[*]") // Should be removed for deployment.
        .config("spark.streaming.receiver.writeAheadLog.enable", "true")
        .config("spark.sql.streaming.checkpointLocation", checkpointLocation)
        .getOrCreate()

    val kafkaStream =
      spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaServers)
        .option("kafka.group.id", groupId)
        .option("subscribe", inTopic)
        .load()

    SparkPipeline().build(kafkaStream)
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServers)
      .option("topic", outTopic)
      .outputMode(OutputMode.Append()) // Does not create any output until the window is closed.
      .start()
      .awaitTermination()

  }

}
