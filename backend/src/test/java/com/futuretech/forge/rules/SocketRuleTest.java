package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.board;
import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.cpu;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.RamType;
import com.futuretech.forge.Issue;
import com.futuretech.forge.Severity;

class SocketRuleTest {

    private final SocketRule rule = new SocketRule();

    @Test
    void sameSocket_isFine() {
        var parts = build().cpu(cpu("AM5", 120, true)).motherboard(board("AM5", RamType.DDR5, FormFactor.ATX)).parts();
        assertThat(rule.check(parts)).isEmpty();
    }

    @Test
    void differentSocket_isAnErrorNamingBothParts() {
        var cpu = cpu("AM5", 120, true);
        var board = board("LGA1851", RamType.DDR5, FormFactor.ATX);

        List<Issue> issues = rule.check(build().cpu(cpu).motherboard(board).parts());

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("SOCKET");
            assertThat(issue.severity()).isEqualTo(Severity.ERROR);
            assertThat(issue.message()).contains("AM5", "LGA1851");
            assertThat(issue.productIds()).containsExactly(cpu.productId(), board.productId());
        });
    }

    @Test
    void skipped_untilBothPartsAreChosen() {
        assertThat(rule.check(build().cpu(cpu("AM5", 120, true)).parts())).isEmpty();
        assertThat(rule.check(build().motherboard(board("LGA1851", RamType.DDR5, FormFactor.ATX)).parts())).isEmpty();
        assertThat(rule.check(build().parts())).isEmpty();
    }
}
