package com.futuretech.dto.admin;

import java.math.BigDecimal;

import com.futuretech.entity.enums.ProductType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Used for both create and update (an update replaces every field). Sizes match the columns in V1. */
public record ProductRequest(
        @NotBlank @Size(max = 50)
        String sku,

        @NotBlank @Size(max = 200)
        String name,

        @NotBlank @Size(max = 100)
        String brand,

        String description,

        @Size(max = 500)
        String imageUrl,

        // NUMERIC(10,2): up to 8 digits before the decimal point
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotNull @PositiveOrZero
        Integer stockQuantity,

        @NotBlank
        String categorySlug,

        @NotNull
        ProductType productType,

        @NotNull @PositiveOrZero
        Integer warrantyMonths,

        @Min(0) @Max(100)
        Integer performanceScore,

        // required for PC parts and computers, must be null for accessories and peripherals
        @Valid
        SpecsRequest specs) {
}
