package com.futuretech.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.futuretech.entity.Product;
import com.futuretech.entity.spec.CaseSpec;
import com.futuretech.entity.spec.CoolerSpec;
import com.futuretech.entity.spec.CpuSpec;
import com.futuretech.entity.spec.GpuSpec;
import com.futuretech.entity.spec.MotherboardSpec;
import com.futuretech.entity.spec.PsuSpec;
import com.futuretech.entity.spec.RamSpec;
import com.futuretech.entity.spec.StorageSpec;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.BuildParts.Part;
import com.futuretech.repository.spec.*;

/** Turns checked products into BuildParts by loading each one's spec row. The only DB code ForgeAI rules need. */
@Component
class BuildPartsLoader {

    private final CpuSpecRepository cpuSpecs;
    private final MotherboardSpecRepository motherboardSpecs;
    private final RamSpecRepository ramSpecs;
    private final GpuSpecRepository gpuSpecs;
    private final StorageSpecRepository storageSpecs;
    private final PsuSpecRepository psuSpecs;
    private final CaseSpecRepository caseSpecs;
    private final CoolerSpecRepository coolerSpecs;

    BuildPartsLoader(CpuSpecRepository cpuSpecs, MotherboardSpecRepository motherboardSpecs,
                     RamSpecRepository ramSpecs, GpuSpecRepository gpuSpecs, StorageSpecRepository storageSpecs,
                     PsuSpecRepository psuSpecs, CaseSpecRepository caseSpecs, CoolerSpecRepository coolerSpecs) {
        this.cpuSpecs = cpuSpecs;
        this.motherboardSpecs = motherboardSpecs;
        this.ramSpecs = ramSpecs;
        this.gpuSpecs = gpuSpecs;
        this.storageSpecs = storageSpecs;
        this.psuSpecs = psuSpecs;
        this.caseSpecs = caseSpecs;
        this.coolerSpecs = coolerSpecs;
    }

    /** Products must already be checked (PC parts only, at most one of each single part). Needs a transaction. */
    BuildParts load(List<Product> products, Map<Long, Integer> quantities) {
        Part<CpuSpec> cpu = null;
        Part<MotherboardSpec> motherboard = null;
        Part<GpuSpec> gpu = null;
        Part<PsuSpec> psu = null;
        Part<CaseSpec> pcCase = null;
        Part<CoolerSpec> cooler = null;
        List<Part<RamSpec>> ram = new ArrayList<>();
        List<Part<StorageSpec>> storage = new ArrayList<>();

        for (Product p : products) {
            int quantity = quantities.get(p.getId());
            switch (p.getProductType()) {
                case CPU -> cpu = part(p, quantity, cpuSpecs);
                case MOTHERBOARD -> motherboard = part(p, quantity, motherboardSpecs);
                case GPU -> gpu = part(p, quantity, gpuSpecs);
                case PSU -> psu = part(p, quantity, psuSpecs);
                case CASE -> pcCase = part(p, quantity, caseSpecs);
                case COOLER -> cooler = part(p, quantity, coolerSpecs);
                case RAM -> ram.add(part(p, quantity, ramSpecs));
                case STORAGE -> storage.add(part(p, quantity, storageSpecs));
                default -> throw new IllegalArgumentException(p.getProductType() + " is not a PC part");
            }
        }
        return new BuildParts(cpu, motherboard, gpu, psu, pcCase, cooler, ram, storage);
    }

    // Every PC part has a spec row (AdminProductService requires it), so a missing one is a data bug
    private static <S> Part<S> part(Product p, int quantity, JpaRepository<S, Long> specs) {
        S spec = specs.findById(p.getId())
                .orElseThrow(() -> new IllegalStateException("Product " + p.getId() + " has no spec row"));
        return new Part<>(p.getId(), p.getName(), quantity, spec);
    }
}
