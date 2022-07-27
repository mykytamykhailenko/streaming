package kafka.model

import play.api.libs.json.{Format, Json}

import scala.math.BigDecimal.RoundingMode

case class Metrics(cpu: BigDecimal,
                   ram: BigDecimal) {

  def +(other: Metrics): Metrics = Metrics(other.cpu + cpu, other.ram + ram)

  def /(count: Int): Metrics = Metrics(cpu / count, ram / count)

  def toAverageMetrics: AverageMetrics = AverageMetrics(cpu, ram, 1)

  def scale: Metrics = Metrics(cpu.setScale(4, RoundingMode.HALF_UP), ram.setScale(4, RoundingMode.HALF_UP))

}

object Metrics {

  implicit val metricsFormat: Format[Metrics] = Json.format[Metrics]

}