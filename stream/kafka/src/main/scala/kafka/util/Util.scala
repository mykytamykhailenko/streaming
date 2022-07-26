package kafka.util

import model.{AverageMetrics, Metrics}
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.{Transformer, TransformerSupplier, Windowed}
import org.apache.kafka.streams.processor.{ProcessorContext, To}

object Util {

  def eventTimeExtractor: Transformer[Windowed[String], AverageMetrics, KeyValue[String, Metrics]] =
    new Transformer[Windowed[String], AverageMetrics, KeyValue[String, Metrics]] {

      var ctx: ProcessorContext = _

      override def init(context: ProcessorContext): Unit = ctx = context

      override def transform(key: Windowed[String], value: AverageMetrics): KeyValue[String, Metrics] = {
        ctx.forward(key.key(), value.toMetrics, To.all().withTimestamp(key.window().start()))
        null
      }

      override def close(): Unit = ()
    }

  val eventTimeExtractorSupplier: TransformerSupplier[Windowed[String], AverageMetrics, KeyValue[String, Metrics]] = () => eventTimeExtractor

}