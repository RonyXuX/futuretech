package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.board;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.ram;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.RamType;

class RamTypeRuleTest {

    private final RamTypeRule rule = new RamTypeRule();

    @Test
    void matchingType_isFine() {
        var parts = build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX))
                .ram(ram(RamType.DDR5, 32, 2, 1)).parts();
        assertThat(rule.check(parts)).isEmpty();
    }

    @Test
    void ddr4KitOnDdr5Board_isAnError() {
        var kit = ram(RamType.DDR4, 32, 2, 1);
        var parts = build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX)).ram(kit).parts();

        assertThat(rule.check(parts)).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("RAM_TYPE");
            assertThat(issue.message()).contains("DDR4", "DDR5");
            assertThat(issue.productIds()).contains(kit.productId());
        });
    }

    @Test
    void oneIssuePerWrongKit_andRightKitsAreNotFlagged() {
        var good = ram(RamType.DDR5, 32, 2, 1);
        var bad1 = ram(RamType.DDR4, 16, 2, 1);
        var bad2 = ram(RamType.DDR4, 8, 1, 1);
        var parts = build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX))
                .ram(good).ram(bad1).ram(bad2).parts();

        assertThat(rule.check(parts)).hasSize(2)
                .noneMatch(issue -> issue.productIds().contains(good.productId()));
    }

    @Test
    void skipped_withoutMotherboard_orWithoutRam() {
        assertThat(rule.check(build().ram(ram(RamType.DDR4, 32, 2, 1)).parts())).isEmpty();
        assertThat(rule.check(build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX)).parts())).isEmpty();
    }
}
