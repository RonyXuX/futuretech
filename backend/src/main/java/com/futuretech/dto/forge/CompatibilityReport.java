package com.futuretech.dto.forge;

import java.math.BigDecimal;
import java.util.List;

import com.futuretech.entity.enums.ProductType;
import com.futuretech.forge.Issue;

/**
 * The result of checking a build.
 *
 * @param compatible       no errors (warnings are allowed)
 * @param complete         every required part is chosen (see missingParts)
 * @param missingParts     required part types not chosen yet; GPU only when the CPU has no integrated graphics
 * @param estimatedWattage CPU + GPU + 100 W; null until a CPU is chosen
 * @param totalPrice       at today's prices
 */
public record CompatibilityReport(
        boolean compatible,
        boolean complete,
        List<Issue> errors,
        List<Issue> warnings,
        List<ProductType> missingParts,
        Integer estimatedWattage,
        BigDecimal totalPrice) {
}
