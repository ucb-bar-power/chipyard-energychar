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
LOADMEM             = 1
CLOCK_PERIOD       = 4.0 # ns, must match what's in custom.yml, vlsi.inputs.clocks
RESET_DELAY        = 499.5  # 500ns delay 

sim ?= $(OBJ_DIR)/sim-rtl-rundir/simv
binary_name := $(notdir $(basename $(BINARY)))

OUTFILE ?= $(output_dir)/$(binary_name).out

sim-rtl-out:
	set -o pipefail &&  $(sim) +permissive +dramsim +dramsim_ini_dir=$(vlsi_dir)/../generators/testchipip/src/main/resources/dramsim2_ini +max-cycles=$(timeout_cycles)  +ntb_random_seed_automatic +verbose +loadmem=$(BINARY) +permissive-off $(BINARY) </dev/null 2> >(spike-dasm > $(OUTFILE)) | tee $(output_dir)/$(binary_name).log

sim-rtl-debug-out:
	set -o pipefail &&  $(sim) +permissive +verbose $(call get_waveform_flag,$(call get_sim_out_name,$(BINARY))) +loadmem=$(BINARY) +dramsim +dramsim_ini_dir=$(vlsi_dir)/../generators/testchipip/src/main/resources/dramsim2_ini +max-cycles=$(timeout_cycles) +permissive-off $(BINARY) </dev/null 2> >(spike-dasm > $(OUTFILE)) | tee $(output_dir)/$(binary_name).log
