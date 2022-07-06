package model

import play.api.libs.json.{Format, Json}

case class MachineWindowed(start: Long,
                           end: Long,
                           cluster: String,
                           machine: String)

object MachineWindowed {

  implicit val machineWindowedFormat: Format[MachineWindowed] = Json.format[MachineWindowed]

}