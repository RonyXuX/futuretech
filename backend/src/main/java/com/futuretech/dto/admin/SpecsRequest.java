package com.futuretech.dto.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.futuretech.entity.enums.CoolerType;
import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.enums.RamType;
import com.futuretech.entity.enums.StorageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Specs sent by the admin. The JSON says which kind it is, e.g. {"kind": "CPU", "socket": "AM5", ...},
 * and Jackson creates the matching record. Sizes match the columns in V1.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SpecsRequest.Cpu.class, name = "CPU"),
        @JsonSubTypes.Type(value = SpecsRequest.Motherboard.class, name = "MOTHERBOARD"),
        @JsonSubTypes.Type(value = SpecsRequest.Ram.class, name = "RAM"),
        @JsonSubTypes.Type(value = SpecsRequest.Gpu.class, name = "GPU"),
        @JsonSubTypes.Type(value = SpecsRequest.Storage.class, name = "STORAGE"),
        @JsonSubTypes.Type(value = SpecsRequest.Psu.class, name = "PSU"),
        @JsonSubTypes.Type(value = SpecsRequest.Case.class, name = "CASE"),
        @JsonSubTypes.Type(value = SpecsRequest.Cooler.class, name = "COOLER"),
        @JsonSubTypes.Type(value = SpecsRequest.Computer.class, name = "COMPUTER")
})
public sealed interface SpecsRequest {

    /** True if these specs belong to products of this type (CPU specs only for CPUs, ...). */
    boolean fits(ProductType type);

    record Cpu(
            @NotBlank @Size(max = 20) String socket,
            @NotNull @Positive Integer cores,
            @NotNull @Positive Integer threads,
            @NotNull @Positive Double baseClockGhz,
            @Positive Double boostClockGhz,
            @NotNull @Positive Integer tdpWatts,
            @NotNull Boolean integratedGraphics) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.CPU;
        }
    }

    record Motherboard(
            @NotBlank @Size(max = 20) String socket,
            @NotBlank @Size(max = 30) String chipset,
            @NotNull FormFactor formFactor,
            @NotNull RamType ramType,
            @NotNull @Positive Integer ramSlots,
            @NotNull @Positive Integer maxRamGb,
            @NotNull @PositiveOrZero Integer m2Slots) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.MOTHERBOARD;
        }
    }

    record Ram(
            @NotNull RamType ramType,
            @NotNull @Positive Integer capacityGb,
            @NotNull @Positive Integer modules,
            @NotNull @Positive Integer speedMhz) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.RAM;
        }
    }

    record Gpu(
            @NotBlank @Size(max = 50) String chipset,
            @NotNull @Positive Integer vramGb,
            @NotNull @Positive Integer lengthMm,
            @NotNull @Positive Integer tdpWatts) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.GPU;
        }
    }

    record Storage(
            @NotNull StorageType storageType,
            @NotNull @Positive Integer capacityGb) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.STORAGE;
        }
    }

    record Psu(
            @NotNull @Positive Integer wattage,
            @NotBlank @Size(max = 30) String efficiencyRating,
            @NotNull Boolean modular,
            @NotNull PsuFormFactor formFactor) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.PSU;
        }
    }

    record Case(
            @NotNull FormFactor maxMotherboardFormFactor,
            @NotNull @Positive Integer maxGpuLengthMm,
            @NotNull @Positive Integer maxCoolerHeightMm,
            @NotNull PsuFormFactor maxPsuFormFactor,
            // 0 = no radiator support
            @NotNull @PositiveOrZero Integer maxRadiatorMm) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.CASE;
        }
    }

    // heightMm only for AIR, radiatorMm only for LIQUID (checked in AdminProductService)
    record Cooler(
            @NotNull CoolerType coolerType,
            @Positive Integer heightMm,
            @Positive Integer radiatorMm,
            @NotEmpty List<@NotBlank @Size(max = 20) String> supportedSockets) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.COOLER;
        }
    }

    record Computer(
            @NotBlank @Size(max = 100) String cpuModel,
            @NotBlank @Size(max = 100) String gpuModel,
            @NotNull @Positive Integer ramGb,
            @NotNull @Positive Integer storageGb,
            @Positive Double screenSizeInches,
            @Size(max = 50) String operatingSystem) implements SpecsRequest {
        public boolean fits(ProductType type) {
            return type == ProductType.LAPTOP || type == ProductType.DESKTOP;
        }
    }
}
