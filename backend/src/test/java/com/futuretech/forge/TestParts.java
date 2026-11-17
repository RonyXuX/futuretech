package com.futuretech.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.futuretech.entity.enums.CoolerType;
import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.enums.RamType;
import com.futuretech.entity.enums.StorageType;
import com.futuretech.entity.spec.CaseSpec;
import com.futuretech.entity.spec.CoolerSpec;
import com.futuretech.entity.spec.CpuSpec;
import com.futuretech.entity.spec.GpuSpec;
import com.futuretech.entity.spec.MotherboardSpec;
import com.futuretech.entity.spec.PsuSpec;
import com.futuretech.entity.spec.RamSpec;
import com.futuretech.entity.spec.StorageSpec;
import com.futuretech.forge.BuildParts.Part;

/** Builds parts and builds from plain spec objects for the rule tests (no database, no mocks). */
public final class TestParts {

    private static final AtomicLong IDS = new AtomicLong(1);

    private TestParts() {
    }

    public static Part<CpuSpec> cpu(String socket, int tdpWatts, boolean integratedGraphics) {
        CpuSpec s = new CpuSpec();
        s.setSocket(socket);
        s.setCores(8);
        s.setThreads(16);
        s.setBaseClockGhz(4.0);
        s.setTdpWatts(tdpWatts);
        s.setIntegratedGraphics(integratedGraphics);
        return part("CPU " + socket, s);
    }

    public static Part<MotherboardSpec> board(String socket, RamType ramType, FormFactor size,
                                              int ramSlots, int maxRamGb) {
        MotherboardSpec s = new MotherboardSpec();
        s.setSocket(socket);
        s.setChipset("X");
        s.setFormFactor(size);
        s.setRamType(ramType);
        s.setRamSlots(ramSlots);
        s.setMaxRamGb(maxRamGb);
        s.setM2Slots(2);
        return part("Board " + socket + " " + size, s);
    }

    public static Part<MotherboardSpec> board(String socket, RamType ramType, FormFactor size) {
        return board(socket, ramType, size, 4, 192);
    }

    public static Part<RamSpec> ram(RamType type, int kitCapacityGb, int modules, int quantity) {
        RamSpec s = new RamSpec();
        s.setRamType(type);
        s.setCapacityGb(kitCapacityGb);
        s.setModules(modules);
        s.setSpeedMhz(6000);
        return new Part<>(IDS.getAndIncrement(), type + " " + kitCapacityGb + "GB kit", quantity, s);
    }

    public static Part<GpuSpec> gpu(int lengthMm, int tdpWatts) {
        GpuSpec s = new GpuSpec();
        s.setChipset("RTX");
        s.setVramGb(12);
        s.setLengthMm(lengthMm);
        s.setTdpWatts(tdpWatts);
        return part("GPU " + lengthMm + "mm", s);
    }

    public static Part<PsuSpec> psu(int wattage, PsuFormFactor size) {
        PsuSpec s = new PsuSpec();
        s.setWattage(wattage);
        s.setEfficiencyRating("80+ Gold");
        s.setModular(true);
        s.setFormFactor(size);
        return part("PSU " + wattage + "W " + size, s);
    }

    public static Part<CaseSpec> pcCase(FormFactor maxBoard, int maxGpuMm, int maxCoolerMm,
                                        PsuFormFactor maxPsu, int maxRadiatorMm) {
        CaseSpec s = new CaseSpec();
        s.setMaxMotherboardFormFactor(maxBoard);
        s.setMaxGpuLengthMm(maxGpuMm);
        s.setMaxCoolerHeightMm(maxCoolerMm);
        s.setMaxPsuFormFactor(maxPsu);
        s.setMaxRadiatorMm(maxRadiatorMm);
        return part("Case " + maxBoard, s);
    }

    /** A roomy ATX case that fits everything, for tests about other rules. */
    public static Part<CaseSpec> bigCase() {
        return pcCase(FormFactor.ATX, 400, 180, PsuFormFactor.ATX, 360);
    }

    public static Part<CoolerSpec> airCooler(int heightMm, String... sockets) {
        CoolerSpec s = new CoolerSpec();
        s.setCoolerType(CoolerType.AIR);
        s.setHeightMm(heightMm);
        s.setSupportedSockets(Set.of(sockets));
        return part("Air cooler " + heightMm + "mm", s);
    }

    public static Part<CoolerSpec> liquidCooler(int radiatorMm, String... sockets) {
        CoolerSpec s = new CoolerSpec();
        s.setCoolerType(CoolerType.LIQUID);
        s.setRadiatorMm(radiatorMm);
        s.setSupportedSockets(Set.of(sockets));
        return part("Liquid cooler " + radiatorMm + "mm", s);
    }

    public static Part<StorageSpec> ssd() {
        StorageSpec s = new StorageSpec();
        s.setStorageType(StorageType.NVME_SSD);
        s.setCapacityGb(1000);
        return part("SSD 1TB", s);
    }

    private static <S> Part<S> part(String name, S spec) {
        return new Part<>(IDS.getAndIncrement(), name, 1, spec);
    }

    public static Build build() {
        return new Build();
    }

    /** Readable way to assemble a BuildParts: build().cpu(...).motherboard(...).parts() */
    public static final class Build {
        private Part<CpuSpec> cpu;
        private Part<MotherboardSpec> motherboard;
        private Part<GpuSpec> gpu;
        private Part<PsuSpec> psu;
        private Part<CaseSpec> pcCase;
        private Part<CoolerSpec> cooler;
        private final List<Part<RamSpec>> ram = new ArrayList<>();
        private final List<Part<StorageSpec>> storage = new ArrayList<>();

        public Build cpu(Part<CpuSpec> p) { cpu = p; return this; }
        public Build motherboard(Part<MotherboardSpec> p) { motherboard = p; return this; }
        public Build gpu(Part<GpuSpec> p) { gpu = p; return this; }
        public Build psu(Part<PsuSpec> p) { psu = p; return this; }
        public Build pcCase(Part<CaseSpec> p) { pcCase = p; return this; }
        public Build cooler(Part<CoolerSpec> p) { cooler = p; return this; }
        public Build ram(Part<RamSpec> p) { ram.add(p); return this; }
        public Build storage(Part<StorageSpec> p) { storage.add(p); return this; }

        public BuildParts parts() {
            return new BuildParts(cpu, motherboard, gpu, psu, pcCase, cooler, ram, storage);
        }
    }
}
