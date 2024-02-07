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


// --------------
// Rocket Configs
// --------------

class EnergyCharRocketConfig extends Config(
  new WithNEnergyCharCores(1) ++         // single rocket-core
  new chipyard.config.WithTileFrequency(250.0) ++    // 4ns period --> 250 MHz freq
  new chipyard.config.WithSystemBusFrequency(250.0) ++   
  new chipyard.config.WithMemoryBusFrequency(250.0) ++   
  new chipyard.config.WithPeripheryBusFrequency(250.0) ++
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
        mulDiv = Some(MulDivParams(
          mulUnroll = 8,
          mulEarlyOut = true,
          divEarlyOut = true)
        )),
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

// ---------------
// Shuttle Configs
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