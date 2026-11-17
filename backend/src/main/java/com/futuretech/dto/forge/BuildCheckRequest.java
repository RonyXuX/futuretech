package com.futuretech.dto.forge;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The parts picked so far. Can be empty: the builder checks live from the very first part. */
public record BuildCheckRequest(
        @NotNull @Size(max = 30, message = "can have at most 30 different products")
        List<@Valid @NotNull Item> items) {

    // 8 covers e.g. 4 RAM kits or several drives; anything above is a typo, not a build
    public record Item(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(8) Integer quantity) {
    }
}
