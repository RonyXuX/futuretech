package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.enums.CoolerType;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/**
 * A tower air cooler must not be taller than the case allows, or the side panel won't close.
 * Liquid coolers have no tower; their radiator is checked by RadiatorFitRule.
 */
@Component
public class CoolerHeightRule implements CompatibilityRule {

    static final String NAME = "COOLER_HEIGHT";

    @Override
    public List<Issue> check(BuildParts build) {
        var cooler = build.cooler();
        var pcCase = build.pcCase();
        if (cooler == null || pcCase == null || cooler.spec().getCoolerType() != CoolerType.AIR) {
            return List.of();
        }
        int height = cooler.spec().getHeightMm();
        int max = pcCase.spec().getMaxCoolerHeightMm();
        if (height <= max) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                cooler.name() + " is " + height + " mm tall, but " + pcCase.name() + " fits coolers up to "
                        + max + " mm.",
                cooler.productId(), pcCase.productId()));
    }
}
