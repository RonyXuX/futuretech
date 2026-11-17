package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.board;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.ram;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.RamType;

class RamSlotsRuleTest {

    private final RamSlotsRule rule = new RamSlotsRule();
    private final RamType ddr5 = RamType.DDR5;

    @Test
    void twoKitsOfTwoSticks_fillFourSlotsExactly() {
        var parts = build().motherboard(board("AM5", ddr5, FormFactor.ATX, 4, 192))
                .ram(ram(ddr5, 32, 2, 2)).parts();   // quantity 2 -> 4 sticks
        assertThat(rule.check(parts)).isEmpty();
    }

    @Test
    void moreSticksThanSlots_isAnError() {
        var parts = build().motherboard(board("AM5", ddr5, FormFactor.MINI_ITX, 2, 96))
                .ram(ram(ddr5, 32, 2, 1)).ram(ram(ddr5, 16, 2, 1)).parts();   // 4 sticks, 2 slots

        assertThat(rule.check(parts)).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("RAM_SLOTS");
            assertThat(issue.message()).contains("4 sticks", "2 slots");
        });
    }

    @Test
    void moreGigabytesThanTheBoardSupports_isAnError() {
        var parts = build().motherboard(board("AM5", ddr5, FormFactor.ATX, 4, 128))
                .ram(ram(ddr5, 96, 2, 2)).parts();   // 4 sticks fit, but 192 GB > 128 GB

        assertThat(rule.check(parts)).singleElement()
                .satisfies(issue -> assertThat(issue.message()).contains("192 GB", "128 GB"));
    }

    @Test
    void bothLimitsBroken_givesTwoIssues() {
        var parts = build().motherboard(board("AM5", ddr5, FormFactor.MINI_ITX, 2, 64))
                .ram(ram(ddr5, 64, 2, 2)).parts();   // 4 sticks in 2 slots, 128 GB > 64 GB
        assertThat(rule.check(parts)).hasSize(2);
    }

    @Test
    void skipped_withoutMotherboard_orWithoutRam() {
        assertThat(rule.check(build().ram(ram(ddr5, 64, 2, 8)).parts())).isEmpty();
        assertThat(rule.check(build().motherboard(board("AM5", ddr5, FormFactor.ATX)).parts())).isEmpty();
    }
}
