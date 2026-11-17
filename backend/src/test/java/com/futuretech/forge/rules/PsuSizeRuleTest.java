package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.pcCase;
import static com.futuretech.forge.TestParts.psu;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;

class PsuSizeRuleTest {

    private final PsuSizeRule rule = new PsuSizeRule();

    @ParameterizedTest(name = "{0} PSU in a case for {1} fits: {2}")
    @CsvSource({
            "SFX, SFX, true",
            "SFX, ATX, true",    // SFX is smaller, it also fits a normal case
            "ATX, SFX, false",
            "ATX, ATX, true"
    })
    void psuSizeAgainstCase(PsuFormFactor psuSize, PsuFormFactor caseMax, boolean fits) {
        var parts = build().psu(psu(750, psuSize)).pcCase(pcCase(FormFactor.MINI_ITX, 330, 70, caseMax, 0)).parts();
        assertThat(rule.check(parts).isEmpty()).isEqualTo(fits);
    }

    @Test
    void error_namesBothSizes() {
        var parts = build().psu(psu(750, PsuFormFactor.ATX))
                .pcCase(pcCase(FormFactor.MINI_ITX, 330, 70, PsuFormFactor.SFX, 0)).parts();
        assertThat(rule.check(parts)).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("PSU_SIZE");
            assertThat(issue.message()).contains("ATX", "SFX");
        });
    }

    @Test
    void skipped_withoutPsu_orWithoutCase() {
        assertThat(rule.check(build().psu(psu(750, PsuFormFactor.ATX)).parts())).isEmpty();
        assertThat(rule.check(build().pcCase(pcCase(FormFactor.MINI_ITX, 330, 70, PsuFormFactor.SFX, 0)).parts()))
                .isEmpty();
    }
}
