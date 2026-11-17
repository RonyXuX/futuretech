package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.cpu;
import static com.futuretech.forge.TestParts.gpu;
import static com.futuretech.forge.TestParts.psu;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.forge.Severity;

/** CPU 120 W + GPU 280 W + 100 W = 400 W draw; 30% headroom -> 520 W recommended. */
class PsuWattageRuleTest {

    private final PsuWattageRule rule = new PsuWattageRule();

    @Test
    void estimatedDraw_isCpuPlusGpuPlus100() {
        assertThat(PsuWattageRule.estimatedDraw(build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 280)).parts()))
                .isEqualTo(500);
        // integrated graphics, no card: only the CPU and the rest
        assertThat(PsuWattageRule.estimatedDraw(build().cpu(cpu("AM5", 65, true)).parts())).isEqualTo(165);
    }

    @Test
    void estimatedDraw_isNullWithoutCpu() {
        assertThat(PsuWattageRule.estimatedDraw(build().gpu(gpu(300, 280)).parts())).isNull();
    }

    @Test
    void enoughHeadroom_isFine() {
        var parts = build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 280)).psu(psu(650, PsuFormFactor.ATX)).parts();
        assertThat(rule.check(parts)).isEmpty();   // 500 W draw, 650 W >= 650 recommended
    }

    @Test
    void exactlyTheRecommendedWattage_isFine() {
        var parts = build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 180)).psu(psu(520, PsuFormFactor.ATX)).parts();
        assertThat(rule.check(parts)).isEmpty();   // 400 W draw x 1.3 = 520 W
    }

    @Test
    void belowRecommendedButAboveDraw_isOnlyAWarning() {
        var parts = build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 180)).psu(psu(450, PsuFormFactor.ATX)).parts();

        assertThat(rule.check(parts)).singleElement().satisfies(issue -> {
            assertThat(issue.severity()).isEqualTo(Severity.WARNING);
            assertThat(issue.message()).contains("450 W", "400 W", "520 W");
        });
    }

    @Test
    void belowTheDraw_isAnError() {
        var parts = build().cpu(cpu("AM5", 170, false)).gpu(gpu(330, 450)).psu(psu(650, PsuFormFactor.ATX)).parts();

        assertThat(rule.check(parts)).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("PSU_WATTAGE");
            assertThat(issue.severity()).isEqualTo(Severity.ERROR);   // 720 W draw > 650 W
        });
    }

    @Test
    void skipped_withoutPsu_orWithoutCpu() {
        assertThat(rule.check(build().cpu(cpu("AM5", 170, false)).gpu(gpu(330, 450)).parts())).isEmpty();
        assertThat(rule.check(build().gpu(gpu(330, 450)).psu(psu(300, PsuFormFactor.ATX)).parts())).isEmpty();
    }
}
