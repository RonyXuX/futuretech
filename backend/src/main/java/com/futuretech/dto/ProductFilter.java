package com.futuretech.dto;

import java.math.BigDecimal;

import com.futuretech.entity.enums.ProductType;

/**
 * The optional filters of GET /api/products. Every field may be null, which means
 * "don't filter on this".
 */
public record ProductFilter(
        String search,         // matches name or brand, case-insensitive
        String categorySlug,
        ProductType type,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock) {
}
