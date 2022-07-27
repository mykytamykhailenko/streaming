package flink.util

import model.Metrics
import org.apache.flink.api.common.eventtime.TimestampAssigner

object Util {

  type EventTime = Long

  type Machine = String

  val theOnlySample = 1

  val timestampAssigner: TimestampAssigner[(EventTime, Machine, Metrics)] =
    (element: (EventTime, Machine, Metrics), _: Long) => {
      val (eventTime, _, _) = element
      eventTime
    }

  val timestampRemover: ((EventTime, Machine, Metrics)) => (Machine, Metrics) = {
    case (_, machine, metrics) => machine -> metrics
  }

}
