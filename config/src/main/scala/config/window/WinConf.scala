package config.window

import com.typesafe.config.{Config, ConfigFactory}

class WinConf extends TWinConf {

  private val config: Config = ConfigFactory.load()

  lazy val windowSize: Int = config.getInt("window.size.ms")

  lazy val windowStep: Int = config.getInt("window.step.ms")

}
