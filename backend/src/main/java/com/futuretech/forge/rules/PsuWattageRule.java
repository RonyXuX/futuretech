package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/**
 * The power supply must deliver what the parts draw, with some headroom.
 *
 * Estimated draw = CPU TDP + GPU TDP + 100 W for everything else (board, RAM, drives, fans).
 * Below the draw: ERROR. Less than 30% above it: WARNING, because a PSU running near its limit
 * is less efficient, louder, and has no room for power spikes.
 */
@Component
public class PsuWattageRule implements CompatibilityRule {

    static final String NAME = "PSU_WATTAGE";
    static final int OTHER_PARTS_WATTS = 100;
    static final int HEADROOM_PERCENT = 30;

    /** Null until a CPU is chosen: without it the estimate would be far too low to mean anything. */
    public static Integer estimatedDraw(BuildParts build) {
        if (build.cpu() == null) {
            return null;
        }
        int gpuWatts = build.gpu() == null ? 0 : build.gpu().spec().getTdpWatts();
        return build.cpu().spec().getTdpWatts() + gpuWatts + OTHER_PARTS_WATTS;
    }

    @Override
    public List<Issue> check(BuildParts build) {
        var psu = build.psu();
        Integer draw = estimatedDraw(build);
        if (psu == null || draw == null) {
            return List.of();
        }
        int wattage = psu.spec().getWattage();
        // whole-number maths: draw * 1.3 as a double can give 520.0000000000001 and round up to 521
        int recommended = Math.ceilDiv(draw * (100 + HEADROOM_PERCENT), 100);
        Long[] ids = involvedIds(build);

        if (wattage < draw) {
            return List.of(Issue.error(NAME,
                    psu.name() + " gives " + wattage + " W, but this build needs about " + draw
                            + " W. Choose at least " + recommended + " W.",
                    ids));
        }
        if (wattage < recommended) {
            return List.of(Issue.warning(NAME,
                    psu.name() + " gives " + wattage + " W for an estimated " + draw
                            + " W. It works, but " + recommended + " W or more leaves safe headroom.",
                    ids));
        }
        return List.of();
    }

    private static Long[] involvedIds(BuildParts build) {
        return build.gpu() == null
                ? new Long[] {build.psu().productId(), build.cpu().productId()}
                : new Long[] {build.psu().productId(), build.cpu().productId(), build.gpu().productId()};
    }
}
