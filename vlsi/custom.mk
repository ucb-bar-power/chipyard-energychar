extra               ?=  # extra configs
args                ?=  # command-line args (including step flow control)

vlsi_dir=$(abspath .)
tech_name          = sky130
INPUT_CONFS        = $(vlsi_dir)/custom.yml $(extra)
ENV_YML            = $(vlsi_dir)/custom-env.yml

HAMMER_EXTRA_ARGS   ?= $(foreach conf, $(INPUT_CONFS), -p $(conf)) $(args)

BINARY 				?= ${RISCV}/riscv64-unknown-elf/share/riscv-tests/benchmarks/towers.riscv

USE_FSDB  		   	= 1

CONFIG				?= EnergyCharRocketConfig

sim ?= $(OBJ_DIR)/sim-rtl-rundir/simv
binary_name := $(notdir $(basename $(BINARY)))

sim-out:
	set -o pipefail &&  $(sim) +permissive +dramsim +dramsim_ini_dir=$(vlsi_dir)/../generators/testchipip/src/main/resources/dramsim2_ini +max-cycles=$(timeout_cycles)  +ntb_random_seed_automatic +verbose +permissive-off $(BINARY) </dev/null 2> >(spike-dasm > $(output_dir)/$(binary_name).out) | tee $(output_dir)/$(binary_name).log
