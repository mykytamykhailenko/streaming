package kafka.model

import play.api.libs.json.{Format, Json}

case class AverageMetrics(cpu: BigDecimal = 0,
                          ram: BigDecimal = 0,
                          samples: Int = 0) {

  def +(am: AverageMetrics): AverageMetrics = AverageMetrics(am.cpu + cpu, am.ram + ram, am.samples + samples)

  def +(m: Metrics): AverageMetrics = AverageMetrics(m.cpu + cpu, m.ram + ram, samples + 1)

  def toMetrics: Metrics = Metrics(cpu / samples, ram / samples)

}

object AverageMetrics {

  implicit val averageMetrics: Format[AverageMetrics] = Json.format[AverageMetrics]

}
