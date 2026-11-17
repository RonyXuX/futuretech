package com.futuretech.forge.rules;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.enums.RamType;
import com.futuretech.entity.spec.RamSpec;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** DDR4 and DDR5 sticks have different notches: each RAM kit must match the motherboard's type. */
@Component
public class RamTypeRule implements CompatibilityRule {

    static final String NAME = "RAM_TYPE";

    @Override
    public List<Issue> check(BuildParts build) {
        var board = build.motherboard();
        if (board == null) {
            return List.of();
        }
        RamType boardType = board.spec().getRamType();
        List<Issue> issues = new ArrayList<>();
        // one issue per wrong kit, so each one can be highlighted on its own
        for (BuildParts.Part<RamSpec> kit : build.ram()) {
            RamType kitType = kit.spec().getRamType();
            if (kitType != boardType) {
                issues.add(Issue.error(NAME,
                        kit.name() + " is " + kitType + " memory, but " + board.name() + " only takes "
                                + boardType + ".",
                        kit.productId(), board.productId()));
            }
        }
        return issues;
    }
}
