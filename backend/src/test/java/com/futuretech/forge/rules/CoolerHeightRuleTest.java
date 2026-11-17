package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.airCooler;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.liquidCooler;
import static com.futuretech.forge.TestParts.pcCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.spec.CaseSpec;
import com.futuretech.forge.BuildParts.Part;

class CoolerHeightRuleTest {

    private final CoolerHeightRule rule = new CoolerHeightRule();

    @Test
    void lowerCooler_fits() {
        assertThat(rule.check(build().cooler(airCooler(155, "AM5")).pcCase(caseWithMaxCooler(165)).parts())).isEmpty();
    }

    @Test
    void coolerExactlyAtTheLimit_fits() {
        assertThat(rule.check(build().cooler(airCooler(165, "AM5")).pcCase(caseWithMaxCooler(165)).parts())).isEmpty();
    }

    @Test
    void tooTallCooler_isAnError() {
        var cooler = airCooler(168, "AM5");
        var pcCase = caseWithMaxCooler(165);

        assertThat(rule.check(build().cooler(cooler).pcCase(pcCase).parts())).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("COOLER_HEIGHT");
            assertThat(issue.message()).contains("168 mm", "165 mm");
            assertThat(issue.productIds()).containsExactly(cooler.productId(), pcCase.productId());
        });
    }

    @Test
    void liquidCoolers_areNotCheckedHere() {
        // no tower to measure; RadiatorFitRule covers liquid coolers
        assertThat(rule.check(build().cooler(liquidCooler(240, "AM5")).pcCase(caseWithMaxCooler(40)).parts())).isEmpty();
    }

    @Test
    void skipped_withoutCooler_orWithoutCase() {
        assertThat(rule.check(build().cooler(airCooler(200, "AM5")).parts())).isEmpty();
        assertThat(rule.check(build().pcCase(caseWithMaxCooler(40)).parts())).isEmpty();
    }

    private static Part<CaseSpec> caseWithMaxCooler(int mm) {
        return pcCase(FormFactor.ATX, 400, mm, PsuFormFactor.ATX, 360);
    }
}
