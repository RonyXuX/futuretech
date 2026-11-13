package com.futuretech.dto;

import java.math.BigDecimal;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;

public record ProductSummaryDto(
        Long id,
        String sku,
        String name,
        String brand,
        BigDecimal price,
        boolean inStock,
        Integer stockQuantity,
        ProductType productType,
        String categorySlug,
        String categoryName,
        Integer performanceScore,
        String imageUrl) {

    /** Needs an open transaction: the category is lazy. */
    public static ProductSummaryDto from(Product p) {
        return new ProductSummaryDto(
                p.getId(), p.getSku(), p.getName(), p.getBrand(), p.getPrice(),
                p.getStockQuantity() > 0, p.getStockQuantity(), p.getProductType(),
                p.getCategory().getSlug(), p.getCategory().getName(),
                p.getPerformanceScore(), p.getImageUrl());
    }
}
