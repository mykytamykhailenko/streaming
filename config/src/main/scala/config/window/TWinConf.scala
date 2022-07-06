package config.window

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[WinConf])
trait TWinConf {

  val windowSize: Int

  val windowStep: Int

}