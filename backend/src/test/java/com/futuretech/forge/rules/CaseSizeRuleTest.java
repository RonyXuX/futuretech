package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.board;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.pcCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.enums.RamType;

class CaseSizeRuleTest {

    private final CaseSizeRule rule = new CaseSizeRule();

    /** Every board size against every case size: smaller boards fit bigger cases, not the reverse. */
    @ParameterizedTest(name = "{0} board in {1} case fits: {2}")
    @CsvSource({
            "MINI_ITX,  MINI_ITX,  true",
            "MINI_ITX,  MICRO_ATX, true",
            "MINI_ITX,  ATX,       true",
            "MICRO_ATX, MINI_ITX,  false",
            "MICRO_ATX, MICRO_ATX, true",
            "MICRO_ATX, ATX,       true",
            "ATX,       MINI_ITX,  false",
            "ATX,       MICRO_ATX, false",
            "ATX,       ATX,       true"
    })
    void boardSizeAgainstCaseSize(FormFactor boardSize, FormFactor caseMax, boolean fits) {
        var parts = build().motherboard(board("AM5", RamType.DDR5, boardSize))
                .pcCase(pcCase(caseMax, 400, 180, PsuFormFactor.ATX, 360)).parts();
        assertThat(rule.check(parts).isEmpty()).isEqualTo(fits);
    }

    @Test
    void error_namesBothSizes() {
        var parts = build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX))
                .pcCase(pcCase(FormFactor.MINI_ITX, 330, 70, PsuFormFactor.SFX, 0)).parts();
        assertThat(rule.check(parts)).singleElement()
                .satisfies(issue -> assertThat(issue.message()).contains("ATX", "MINI_ITX"));
    }

    @Test
    void skipped_withoutBoard_orWithoutCase() {
        assertThat(rule.check(build().motherboard(board("AM5", RamType.DDR5, FormFactor.ATX)).parts())).isEmpty();
        assertThat(rule.check(build().pcCase(pcCase(FormFactor.MINI_ITX, 330, 70, PsuFormFactor.SFX, 0)).parts()))
                .isEmpty();
    }
}
