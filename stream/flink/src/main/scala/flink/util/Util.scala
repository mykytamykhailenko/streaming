package flink.util

import model.Metrics
import org.apache.flink.api.common.eventtime.TimestampAssigner

object Util {

  type EventTime = Long

  val theOnlySample = 1

  val timestampAssigner: TimestampAssigner[(EventTime, String, Metrics)] =
    (element: (EventTime, String, Metrics), _: Long) => {
      val (eventTime, _, _) = element
      eventTime
    }

  val timestampRemover: ((EventTime, String, Metrics)) => (String, Metrics) = {
    case (_, machine, metrics) => machine -> metrics
  }

}
