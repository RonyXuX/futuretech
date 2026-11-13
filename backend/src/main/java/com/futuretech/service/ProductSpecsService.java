package com.futuretech.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.futuretech.dto.ProductSpecsDto;
import com.futuretech.entity.Product;
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
}
