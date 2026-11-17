package com.futuretech.forge;

import java.util.List;

import com.futuretech.entity.spec.CaseSpec;
import com.futuretech.entity.spec.CoolerSpec;
import com.futuretech.entity.spec.CpuSpec;
import com.futuretech.entity.spec.GpuSpec;
import com.futuretech.entity.spec.MotherboardSpec;
import com.futuretech.entity.spec.PsuSpec;
import com.futuretech.entity.spec.RamSpec;
import com.futuretech.entity.spec.StorageSpec;

/**
 * The parts of one PC build, already loaded with their specs. The rules only read this record and
 * never touch the database, so each rule can be unit-tested with plain objects.
 * A single part that isn't chosen yet is null; RAM and storage are lists (empty if none chosen).
 */
public record BuildParts(
        Part<CpuSpec> cpu,
        Part<MotherboardSpec> motherboard,
        Part<GpuSpec> gpu,
        Part<PsuSpec> psu,
        Part<CaseSpec> pcCase,
        Part<CoolerSpec> cooler,
        List<Part<RamSpec>> ram,
        List<Part<StorageSpec>> storage) {

    /** One chosen product: what the messages need (id, name), how many, and its specs. */
    public record Part<S>(Long productId, String name, int quantity, S spec) {
    }

    public BuildParts {
        ram = ram == null ? List.of() : List.copyOf(ram);
        storage = storage == null ? List.of() : List.copyOf(storage);
    }
}
