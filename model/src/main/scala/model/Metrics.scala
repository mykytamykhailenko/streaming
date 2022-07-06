package model

import play.api.libs.json.{Format, Json}

import scala.math.BigDecimal.RoundingMode

case class Metrics(cpu: BigDecimal,
                   ram: BigDecimal) {
  def +(other: Metrics): Metrics = other match {
    case Metrics(otherCpu, otherRam) => Metrics(cpu + otherCpu, ram + otherRam)
  }

  def /(other: Metrics): Metrics = other match {
    case Metrics(otherCpu, otherRam) => Metrics(cpu / otherCpu, ram / otherRam)
  }

  def scale: Metrics = Metrics(cpu.setScale(4, RoundingMode.HALF_UP), ram.setScale(4, RoundingMode.HALF_UP))

}

object Metrics {

  implicit val metricsFormat: Format[Metrics] = Json.format[Metrics]

}