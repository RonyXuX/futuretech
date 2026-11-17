package com.futuretech.forge.rules;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.spec.MotherboardSpec;
import com.futuretech.entity.spec.RamSpec;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;
import com.futuretech.forge.Severity;

/**
 * All RAM sticks must fit in the motherboard's slots, and the total must not exceed its maximum.
 * A kit's capacityGb is the whole kit (a "32GB (2x16GB)" kit has capacityGb 32 and modules 2).
 */
@Component
public class RamSlotsRule implements CompatibilityRule {

    static final String NAME = "RAM_SLOTS";

    @Override
    public List<Issue> check(BuildParts build) {
        var board = build.motherboard();
        if (board == null || build.ram().isEmpty()) {
            return List.of();
        }
        MotherboardSpec spec = board.spec();
        int sticks = 0;
        int totalGb = 0;
        List<Long> ids = new ArrayList<>();
        ids.add(board.productId());
        for (BuildParts.Part<RamSpec> kit : build.ram()) {
            sticks += kit.spec().getModules() * kit.quantity();
            totalGb += kit.spec().getCapacityGb() * kit.quantity();
            ids.add(kit.productId());
        }

        List<Issue> issues = new ArrayList<>();
        if (sticks > spec.getRamSlots()) {
            issues.add(new Issue(NAME, Severity.ERROR,
                    "These RAM kits have " + sticks + " sticks, but " + board.name() + " has only "
                            + spec.getRamSlots() + " slots.",
                    ids));
        }
        if (totalGb > spec.getMaxRamGb()) {
            issues.add(new Issue(NAME, Severity.ERROR,
                    "These RAM kits add up to " + totalGb + " GB, but " + board.name() + " supports at most "
                            + spec.getMaxRamGb() + " GB.",
                    ids));
        }
        return issues;
    }
}
