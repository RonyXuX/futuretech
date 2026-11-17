package com.futuretech.forge.rules;

import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.gpu;
import static com.futuretech.forge.TestParts.pcCase;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.PsuFormFactor;
import com.futuretech.entity.spec.CaseSpec;
import com.futuretech.forge.BuildParts.Part;

class GpuLengthRuleTest {

    private final GpuLengthRule rule = new GpuLengthRule();

    @Test
    void shorterCard_fits() {
        assertThat(rule.check(build().gpu(gpu(300, 250)).pcCase(caseWithMaxGpu(340)).parts())).isEmpty();
    }

    @Test
    void cardExactlyAtTheLimit_fits() {
        assertThat(rule.check(build().gpu(gpu(340, 250)).pcCase(caseWithMaxGpu(340)).parts())).isEmpty();
    }

    @Test
    void oneMillimetreTooLong_isAnError() {
        var card = gpu(341, 250);
        var pcCase = caseWithMaxGpu(340);

        assertThat(rule.check(build().gpu(card).pcCase(pcCase).parts())).singleElement().satisfies(issue -> {
            assertThat(issue.rule()).isEqualTo("GPU_LENGTH");
            assertThat(issue.message()).contains("341 mm", "340 mm");
            assertThat(issue.productIds()).containsExactly(card.productId(), pcCase.productId());
        });
    }

    @Test
    void skipped_withoutGpu_orWithoutCase() {
        assertThat(rule.check(build().gpu(gpu(400, 250)).parts())).isEmpty();
        assertThat(rule.check(build().pcCase(caseWithMaxGpu(200)).parts())).isEmpty();
    }

    private static Part<CaseSpec> caseWithMaxGpu(int mm) {
        return pcCase(FormFactor.ATX, mm, 180, PsuFormFactor.ATX, 360);
    }
}
