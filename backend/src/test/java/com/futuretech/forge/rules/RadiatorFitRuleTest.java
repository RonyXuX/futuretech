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

class RadiatorFitRuleTest {

    private final RadiatorFitRule rule = new RadiatorFitRule();

    @Test
    void smallerRadiator_fits() {
        assertThat(rule.check(build().cooler(liquidCooler(240, "AM5")).pcCase(caseWithMaxRadiator(360)).parts()))
                .isEmpty();
    }

    @Test
    void radiatorExactlyAtTheLimit_fits() {
        assertThat(rule.check(build().cooler(liquidCooler(360, "AM5")).pcCase(caseWithMaxRadiator(360)).parts()))
                .isEmpty();
    }

    @Test
    void tooBigRadiator_isAnError() {
        var cooler = liquidCooler(360, "AM5");
        var pcCase = caseWithMaxRadiator(280);

        assertThat(rule.check(build().cooler(cooler).pcCase(pcCase).parts())).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("RADIATOR_FIT");
            assertThat(issue.message()).contains("360 mm", "280 mm");
            assertThat(issue.productIds()).containsExactly(cooler.productId(), pcCase.productId());
        });
    }

    @Test
    void caseWithoutRadiatorMounts_saysSo() {
        var parts = build().cooler(liquidCooler(240, "AM5")).pcCase(caseWithMaxRadiator(0)).parts();
        assertThat(rule.check(parts)).singleElement()
                .satisfies(issue -> assertThat(issue.message()).contains("no radiator mounts"));
    }

    @Test
    void airCoolers_areNotCheckedHere() {
        assertThat(rule.check(build().cooler(airCooler(155, "AM5")).pcCase(caseWithMaxRadiator(0)).parts())).isEmpty();
    }

    @Test
    void skipped_withoutCooler_orWithoutCase() {
        assertThat(rule.check(build().cooler(liquidCooler(360, "AM5")).parts())).isEmpty();
        assertThat(rule.check(build().pcCase(caseWithMaxRadiator(0)).parts())).isEmpty();
    }

    private static Part<CaseSpec> caseWithMaxRadiator(int mm) {
        return pcCase(FormFactor.ATX, 400, 180, PsuFormFactor.ATX, mm);
    }
}
