package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** The graphics card must not be longer than the space the case has for it. */
@Component
public class GpuLengthRule implements CompatibilityRule {

    static final String NAME = "GPU_LENGTH";

    @Override
    public List<Issue> check(BuildParts build) {
        var gpu = build.gpu();
        var pcCase = build.pcCase();
        if (gpu == null || pcCase == null) {
            return List.of();
        }
        int length = gpu.spec().getLengthMm();
        int max = pcCase.spec().getMaxGpuLengthMm();
        if (length <= max) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                gpu.name() + " is " + length + " mm long, but " + pcCase.name() + " fits cards up to "
                        + max + " mm.",
                gpu.productId(), pcCase.productId()));
    }
}
