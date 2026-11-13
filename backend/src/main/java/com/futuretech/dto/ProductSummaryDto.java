package com.futuretech.dto;

import java.math.BigDecimal;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;

/** The short version of a product, used in product lists (no description, no specs). */
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

    /** Must be called inside a transaction, because product.getCategory() is loaded lazily. */
    public static ProductSummaryDto from(Product p) {
        return new ProductSummaryDto(
                p.getId(), p.getSku(), p.getName(), p.getBrand(), p.getPrice(),
                p.getStockQuantity() > 0, p.getStockQuantity(), p.getProductType(),
                p.getCategory().getSlug(), p.getCategory().getName(),
                p.getPerformanceScore(), p.getImageUrl());
    }
}
