package model

import play.api.libs.json.{Format, Json}

case class Metrics(cpu: BigDecimal,
                   ram: BigDecimal)

object Metrics {

  implicit val metricsFormat: Format[Metrics] = Json.format[Metrics]

}
