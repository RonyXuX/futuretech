package com.futuretech.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.futuretech.dto.ProductSpecsDto;
import com.futuretech.dto.admin.SpecsRequest;
import com.futuretech.entity.Product;
import com.futuretech.entity.spec.*;
import com.futuretech.repository.spec.*;

@Service
public class ProductSpecsService {

    private final CpuSpecRepository cpuSpecs;
    private final MotherboardSpecRepository motherboardSpecs;
    private final RamSpecRepository ramSpecs;
    private final GpuSpecRepository gpuSpecs;
    private final StorageSpecRepository storageSpecs;
    private final PsuSpecRepository psuSpecs;
    private final CaseSpecRepository caseSpecs;
    private final CoolerSpecRepository coolerSpecs;
    private final ComputerSpecRepository computerSpecs;

    public ProductSpecsService(CpuSpecRepository cpuSpecs, MotherboardSpecRepository motherboardSpecs,
                               RamSpecRepository ramSpecs, GpuSpecRepository gpuSpecs,
                               StorageSpecRepository storageSpecs, PsuSpecRepository psuSpecs,
                               CaseSpecRepository caseSpecs, CoolerSpecRepository coolerSpecs,
                               ComputerSpecRepository computerSpecs) {
        this.cpuSpecs = cpuSpecs;
        this.motherboardSpecs = motherboardSpecs;
        this.ramSpecs = ramSpecs;
        this.gpuSpecs = gpuSpecs;
        this.storageSpecs = storageSpecs;
        this.psuSpecs = psuSpecs;
        this.caseSpecs = caseSpecs;
        this.coolerSpecs = coolerSpecs;
        this.computerSpecs = computerSpecs;
    }

    /** null if the type has no spec table (or the row is missing). Needs an open transaction. */
    public ProductSpecsDto findSpecs(Product product) {
        Long id = product.getId();
        Optional<? extends ProductSpecsDto> specs = switch (product.getProductType()) {
            case CPU -> cpuSpecs.findById(id).map(ProductSpecsDto.Cpu::from);
            case MOTHERBOARD -> motherboardSpecs.findById(id).map(ProductSpecsDto.Motherboard::from);
            case RAM -> ramSpecs.findById(id).map(ProductSpecsDto.Ram::from);
            case GPU -> gpuSpecs.findById(id).map(ProductSpecsDto.Gpu::from);
            case STORAGE -> storageSpecs.findById(id).map(ProductSpecsDto.Storage::from);
            case PSU -> psuSpecs.findById(id).map(ProductSpecsDto.Psu::from);
            case CASE -> caseSpecs.findById(id).map(ProductSpecsDto.Case::from);
            case COOLER -> coolerSpecs.findById(id).map(ProductSpecsDto.Cooler::from);
            case LAPTOP, DESKTOP -> computerSpecs.findById(id).map(ProductSpecsDto.Computer::from);
            case ACCESSORY, PERIPHERAL -> Optional.empty();
        };
        return specs.orElse(null);
    }

    /**
     * Creates the spec row, or overwrites it if the product already has one.
     * The product must already be saved (the spec reuses its id). Needs an open transaction.
     */
    public void saveSpecs(Product product, SpecsRequest request) {
        Long id = product.getId();
        switch (request) {
            case SpecsRequest.Cpu r -> {
                CpuSpec s = cpuSpecs.findById(id).orElseGet(CpuSpec::new);
                s.setProduct(product);
                s.setSocket(socket(r.socket()));
                s.setCores(r.cores());
                s.setThreads(r.threads());
                s.setBaseClockGhz(r.baseClockGhz());
                s.setBoostClockGhz(r.boostClockGhz());
                s.setTdpWatts(r.tdpWatts());
                s.setIntegratedGraphics(r.integratedGraphics());
                cpuSpecs.save(s);
            }
            case SpecsRequest.Motherboard r -> {
                MotherboardSpec s = motherboardSpecs.findById(id).orElseGet(MotherboardSpec::new);
                s.setProduct(product);
                s.setSocket(socket(r.socket()));
                s.setChipset(r.chipset().trim());
                s.setFormFactor(r.formFactor());
                s.setRamType(r.ramType());
                s.setRamSlots(r.ramSlots());
                s.setMaxRamGb(r.maxRamGb());
                s.setM2Slots(r.m2Slots());
                motherboardSpecs.save(s);
            }
            case SpecsRequest.Ram r -> {
                RamSpec s = ramSpecs.findById(id).orElseGet(RamSpec::new);
                s.setProduct(product);
                s.setRamType(r.ramType());
                s.setCapacityGb(r.capacityGb());
                s.setModules(r.modules());
                s.setSpeedMhz(r.speedMhz());
                ramSpecs.save(s);
            }
            case SpecsRequest.Gpu r -> {
                GpuSpec s = gpuSpecs.findById(id).orElseGet(GpuSpec::new);
                s.setProduct(product);
                s.setChipset(r.chipset().trim());
                s.setVramGb(r.vramGb());
                s.setLengthMm(r.lengthMm());
                s.setTdpWatts(r.tdpWatts());
                gpuSpecs.save(s);
            }
            case SpecsRequest.Storage r -> {
                StorageSpec s = storageSpecs.findById(id).orElseGet(StorageSpec::new);
                s.setProduct(product);
                s.setStorageType(r.storageType());
                s.setCapacityGb(r.capacityGb());
                storageSpecs.save(s);
            }
            case SpecsRequest.Psu r -> {
                PsuSpec s = psuSpecs.findById(id).orElseGet(PsuSpec::new);
                s.setProduct(product);
                s.setWattage(r.wattage());
                s.setEfficiencyRating(r.efficiencyRating().trim());
                s.setModular(r.modular());
                s.setFormFactor(r.formFactor());
                psuSpecs.save(s);
            }
            case SpecsRequest.Case r -> {
                CaseSpec s = caseSpecs.findById(id).orElseGet(CaseSpec::new);
                s.setProduct(product);
                s.setMaxMotherboardFormFactor(r.maxMotherboardFormFactor());
                s.setMaxGpuLengthMm(r.maxGpuLengthMm());
                s.setMaxCoolerHeightMm(r.maxCoolerHeightMm());
                s.setMaxPsuFormFactor(r.maxPsuFormFactor());
                s.setMaxRadiatorMm(r.maxRadiatorMm());
                caseSpecs.save(s);
            }
            case SpecsRequest.Cooler r -> {
                CoolerSpec s = coolerSpecs.findById(id).orElseGet(CoolerSpec::new);
                s.setProduct(product);
                s.setCoolerType(r.coolerType());
                s.setHeightMm(r.heightMm());
                s.setRadiatorMm(r.radiatorMm());
                // clear + add (not a new Set) so Hibernate updates the existing socket rows
                s.getSupportedSockets().clear();
                r.supportedSockets().forEach(socket -> s.getSupportedSockets().add(socket(socket)));
                coolerSpecs.save(s);
            }
            case SpecsRequest.Computer r -> {
                ComputerSpec s = computerSpecs.findById(id).orElseGet(ComputerSpec::new);
                s.setProduct(product);
                s.setCpuModel(r.cpuModel().trim());
                s.setGpuModel(r.gpuModel().trim());
                s.setRamGb(r.ramGb());
                s.setStorageGb(r.storageGb());
                s.setScreenSizeInches(r.screenSizeInches());
                s.setOperatingSystem(r.operatingSystem() == null ? null : r.operatingSystem().trim());
                computerSpecs.save(s);
            }
        }
    }

    // The socket rule compares these strings exactly, so " am5" must be stored as "AM5"
    private static String socket(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
