package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** Small cases only take the smaller SFX power supplies; an ATX one won't fit. */
@Component
public class PsuSizeRule implements CompatibilityRule {

    static final String NAME = "PSU_SIZE";

    @Override
    public List<Issue> check(BuildParts build) {
        var psu = build.psu();
        var pcCase = build.pcCase();
        if (psu == null || pcCase == null) {
            return List.of();
        }
        PsuFormFactor psuSize = psu.spec().getFormFactor();
        PsuFormFactor caseMax = pcCase.spec().getMaxPsuFormFactor();
        if (psuSize.fitsIn(caseMax)) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                psu.name() + " is an " + psuSize + " power supply, but " + pcCase.name() + " only fits "
                        + caseMax + ".",
                psu.productId(), pcCase.productId()));
    }
}
