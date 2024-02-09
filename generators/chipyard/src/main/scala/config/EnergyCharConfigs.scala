package chipyard

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.diplomacy.{AsynchronousCrossing}


// for EnergyCharCore
import chisel3.util._
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem._

// for Shuttle Core
import shuttle.common._
// import chisel3._

// for TileFragments
import freechips.rocketchip.tile._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.rocket.{RocketCoreParams, MulDivParams, DCacheParams, ICacheParams}

import boom.common.{BoomTileAttachParams}
import cva6.{CVA6TileAttachParams}
import testchipip._



// --------------
// Rocket Configs
// based on RocketConfigs.scala
// --------------

class EnergyCharRocketConfigCG extends Config(
  new WithClockGating ++ 
  new WithNEnergyCharCores(1) ++         // single rocket-core
  new chipyard.config.WithTileFrequency(66.66666666667) ++              // 15ns period --> 67 MHz freq
  new chipyard.config.WithSystemBusFrequency(66.66666666667) ++         // include many digits so that 1/f rounds to exactly 15.0ns
  new chipyard.config.WithMemoryBusFrequency(66.66666666667) ++   
  new chipyard.config.WithPeripheryBusFrequency(66.66666666667) ++
  new chipyard.config.AbstractConfig)

class EnergyCharRocketConfig extends Config(
  new WithNEnergyCharCores(1) ++         // single rocket-core
  new chipyard.config.WithTileFrequency(66.66666666667) ++  
  new chipyard.config.WithSystemBusFrequency(66.66666666667) ++   
  new chipyard.config.WithMemoryBusFrequency(66.66666666667) ++   
  new chipyard.config.WithPeripheryBusFrequency(66.66666666667) ++
  new chipyard.config.AbstractConfig)

class WithNEnergyCharCores(
  n: Int,
  overrideIdOffset: Option[Int] = None,
  crossing: RocketCrossingParams = RocketCrossingParams()
) extends Config((site, here, up) => {
  case TilesLocated(InSubsystem) => {
    val prev = up(TilesLocated(InSubsystem), site)
    val idOffset = overrideIdOffset.getOrElse(prev.size)
    val big = RocketTileParams(
      core   = RocketCoreParams(
        useVM = false,
        mulDiv = Some(MulDivParams(
          mulUnroll = 8,
          mulEarlyOut = true,
          divEarlyOut = true)
        )),
      btb = None,
      dcache = Some(DCacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nMSHRs = 0,
        blockBytes = site(CacheBlockBytes))),
      icache = Some(ICacheParams(
        rowBits = site(SystemBusKey).beatBits,
        blockBytes = site(CacheBlockBytes))))
    List.tabulate(n)(i => RocketTileAttachParams(
      big.copy(hartId = i + idOffset),
      crossing
    )) ++ prev
  }
})

// --------------
// CVA6 Configs
// based on CVA6Configs.scala
// --------------

class EnergyCharCVA6Config extends Config(
  new cva6.WithNCVA6Cores(1) ++                    // single CVA6 core
  new chipyard.config.WithTileFrequency(66.66666666667) ++    // 13ns period --> 77 MHz freq
  new chipyard.config.WithSystemBusFrequency(66.66666666667) ++   // TODO: check what a synthesizable clock frequency is for CVA6!!
  new chipyard.config.WithMemoryBusFrequency(66.66666666667) ++   
  new chipyard.config.WithPeripheryBusFrequency(66.66666666667) ++
  new chipyard.config.AbstractConfig)



// ---------------
// Shuttle Configs
// based on ShuttleConfigs.scala
// ---------------


class EnergyCharShuttleConfig extends Config(
  new WithNEnergyCharShuttleCores ++                        // 1x dual-issue shuttle core
  new chipyard.config.AbstractConfig)


class WithNEnergyCharShuttleCores(n: Int = 1, retireWidth: Int = 2, overrideIdOffset: Option[Int] = None) extends Config((site, here, up) => {
  case TilesLocated(InSubsystem) => {
    val prev = up(TilesLocated(InSubsystem), site)
    val idOffset = overrideIdOffset.getOrElse(prev.size)
      (0 until n).map { i =>
        ShuttleTileAttachParams(
          tileParams = ShuttleTileParams(
            core = ShuttleCoreParams(retireWidth = retireWidth),
            btb = Some(BTBParams(nEntries=32)),
            dcache = Some(
              DCacheParams(
                rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=8,
                nMSHRs=4) // remove miss status holding registers to use DCache instead of NonBlockingDCache (bc we only have single-ported memories)
                          // BUT we can't because Shuttle won't get generated :(
            ),
            icache = Some(
              ICacheParams(rowBits = -1, nSets=64, nWays=8, fetchBytes=2*4)
            ),
            hartId = i + idOffset
          ),
          crossingParams = RocketCrossingParams()
        )
      } ++ prev
    }
    case XLen => 64
})




// --------------
// Tile Fragments
// based on fragments/TileFragments.scala
// --------------

class WithClockGating extends Config((site, here, up) => {
  case TilesLocated(InSubsystem) => up(TilesLocated(InSubsystem), site) map {
    case tp: RocketTileAttachParams => tp.copy(tileParams = tp.tileParams.copy(
      core = tp.tileParams.core.copy(clockGate = true)))
    case tp: BoomTileAttachParams => tp.copy(tileParams = tp.tileParams.copy(
      core = tp.tileParams.core.copy(clockGate = true)))
    case other => other
  }
})
