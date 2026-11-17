package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.enums.CoolerType;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** A liquid cooler's radiator (e.g. 240 or 360 mm) must fit the largest one the case can mount. */
@Component
public class RadiatorFitRule implements CompatibilityRule {

    static final String NAME = "RADIATOR_FIT";

    @Override
    public List<Issue> check(BuildParts build) {
        var cooler = build.cooler();
        var pcCase = build.pcCase();
        if (cooler == null || pcCase == null || cooler.spec().getCoolerType() != CoolerType.LIQUID) {
            return List.of();
        }
        int radiator = cooler.spec().getRadiatorMm();
        int max = pcCase.spec().getMaxRadiatorMm();
        if (radiator <= max) {
            return List.of();
        }
        String caseLimit = max == 0
                ? pcCase.name() + " has no radiator mounts"
                : pcCase.name() + " fits radiators up to " + max + " mm";
        return List.of(Issue.error(NAME,
                cooler.name() + " has a " + radiator + " mm radiator, but " + caseLimit + ".",
                cooler.productId(), pcCase.productId()));
    }
}
