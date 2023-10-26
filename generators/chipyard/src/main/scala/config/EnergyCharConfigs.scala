package chipyard

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing}

// --------------
// Rocket Configs
// --------------

class EnergyCharRocketConfig extends Config(
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++         // single rocket-core
  new chipyard.config.WithTileFrequency(250.0) ++    // 4ns period --> 250 MHz freq
  new chipyard.config.WithSystemBusFrequency(250.0) ++   
  new chipyard.config.WithMemoryBusFrequency(250.0) ++   
  new chipyard.config.WithPeripheryBusFrequency(250.0) ++
  new chipyard.config.AbstractConfig)
