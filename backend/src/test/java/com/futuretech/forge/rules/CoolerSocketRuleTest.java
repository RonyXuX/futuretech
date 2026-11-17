package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.airCooler;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.cpu;
import static com.futuretech.forge.TestParts.liquidCooler;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoolerSocketRuleTest {

    private final CoolerSocketRule rule = new CoolerSocketRule();

    @Test
    void coolerSupportingTheCpuSocket_isFine() {
        var parts = build().cpu(cpu("LGA1851", 125, true)).cooler(airCooler(155, "AM4", "AM5", "LGA1851")).parts();
        assertThat(rule.check(parts)).isEmpty();
    }

    @Test
    void coolerWithoutTheCpuSocket_isAnError() {
        var cpu = cpu("LGA1851", 125, true);
        var cooler = airCooler(155, "AM4", "AM5");

        assertThat(rule.check(build().cpu(cpu).cooler(cooler).parts())).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("COOLER_SOCKET");
            assertThat(issue.message()).contains("LGA1851");
            assertThat(issue.productIds()).containsExactly(cooler.productId(), cpu.productId());
        });
    }

    @Test
    void liquidCoolers_areCheckedToo() {
        var parts = build().cpu(cpu("AM5", 120, true)).cooler(liquidCooler(360, "LGA1700")).parts();
        assertThat(rule.check(parts)).hasSize(1);
    }

    @Test
    void skipped_withoutCpu_orWithoutCooler() {
        assertThat(rule.check(build().cooler(airCooler(155, "AM4")).parts())).isEmpty();
        assertThat(rule.check(build().cpu(cpu("AM5", 120, true)).parts())).isEmpty();
    }
}
