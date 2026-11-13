package com.futuretech.dto;

import java.util.List;

import com.futuretech.entity.enums.CoolerType;
import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.enums.RamType;
import com.futuretech.entity.enums.StorageType;
import com.futuretech.entity.spec.*;

public sealed interface ProductSpecsDto {

    record Cpu(String socket, int cores, int threads, double baseClockGhz, Double boostClockGhz,
               int tdpWatts, boolean integratedGraphics) implements ProductSpecsDto {
        public static Cpu from(CpuSpec s) {
            return new Cpu(s.getSocket(), s.getCores(), s.getThreads(), s.getBaseClockGhz(),
                    s.getBoostClockGhz(), s.getTdpWatts(), s.getIntegratedGraphics());
        }
    }

    record Motherboard(String socket, String chipset, FormFactor formFactor, RamType ramType,
                       int ramSlots, int maxRamGb, int m2Slots) implements ProductSpecsDto {
        public static Motherboard from(MotherboardSpec s) {
            return new Motherboard(s.getSocket(), s.getChipset(), s.getFormFactor(), s.getRamType(),
                    s.getRamSlots(), s.getMaxRamGb(), s.getM2Slots());
        }
    }

    record Ram(RamType ramType, int capacityGb, int modules, int speedMhz) implements ProductSpecsDto {
        public static Ram from(RamSpec s) {
            return new Ram(s.getRamType(), s.getCapacityGb(), s.getModules(), s.getSpeedMhz());
        }
    }

    record Gpu(String chipset, int vramGb, int lengthMm, int tdpWatts) implements ProductSpecsDto {
        public static Gpu from(GpuSpec s) {
            return new Gpu(s.getChipset(), s.getVramGb(), s.getLengthMm(), s.getTdpWatts());
        }
    }

    record Storage(StorageType storageType, int capacityGb) implements ProductSpecsDto {
        public static Storage from(StorageSpec s) {
            return new Storage(s.getStorageType(), s.getCapacityGb());
        }
    }

    record Psu(int wattage, String efficiencyRating, boolean modular, PsuFormFactor formFactor)
            implements ProductSpecsDto {
        public static Psu from(PsuSpec s) {
            return new Psu(s.getWattage(), s.getEfficiencyRating(), s.getModular(), s.getFormFactor());
        }
    }

    record Case(FormFactor maxMotherboardFormFactor, int maxGpuLengthMm, int maxCoolerHeightMm,
                PsuFormFactor maxPsuFormFactor, int maxRadiatorMm) implements ProductSpecsDto {
        public static Case from(CaseSpec s) {
            return new Case(s.getMaxMotherboardFormFactor(), s.getMaxGpuLengthMm(), s.getMaxCoolerHeightMm(),
                    s.getMaxPsuFormFactor(), s.getMaxRadiatorMm());
        }
    }

    record Cooler(CoolerType coolerType, Integer heightMm, Integer radiatorMm, List<String> supportedSockets)
            implements ProductSpecsDto {
        public static Cooler from(CoolerSpec s) {
            // sorted so the JSON is always in the same order
            return new Cooler(s.getCoolerType(), s.getHeightMm(), s.getRadiatorMm(),
                    s.getSupportedSockets().stream().sorted().toList());
        }
    }

    record Computer(String cpuModel, String gpuModel, int ramGb, int storageGb, Double screenSizeInches,
                    String operatingSystem) implements ProductSpecsDto {
        public static Computer from(ComputerSpec s) {
            return new Computer(s.getCpuModel(), s.getGpuModel(), s.getRamGb(), s.getStorageGb(),
                    s.getScreenSizeInches(), s.getOperatingSystem());
        }
    }
}
