package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** The motherboard must fit the case: smaller boards fit bigger cases, not the other way round. */
@Component
public class CaseSizeRule implements CompatibilityRule {

    static final String NAME = "CASE_SIZE";

    @Override
    public List<Issue> check(BuildParts build) {
        var board = build.motherboard();
        var pcCase = build.pcCase();
        if (board == null || pcCase == null) {
            return List.of();
        }
        FormFactor boardSize = board.spec().getFormFactor();
        FormFactor caseMax = pcCase.spec().getMaxMotherboardFormFactor();
        if (boardSize.fitsIn(caseMax)) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                board.name() + " is " + boardSize + ", but " + pcCase.name() + " only fits boards up to "
                        + caseMax + ".",
                board.productId(), pcCase.productId()));
    }
}
