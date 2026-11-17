package com.futuretech.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Positive adds points, negative takes them away. The limits only catch typos like an extra zero. */
public record PointsAdjustmentRequest(@NotNull @Min(-100_000) @Max(100_000) Integer amount) {
}
