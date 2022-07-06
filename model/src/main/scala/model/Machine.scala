package model

import play.api.libs.json.{Format, Json}

case class Machine(cluster: String, machine: String)

object Machine {

  implicit val machineFormat: Format[Machine] = Json.format[Machine]

}