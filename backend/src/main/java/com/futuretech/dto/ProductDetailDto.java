package com.futuretech.dto;

import java.math.BigDecimal;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;

/**
 * The full product, as shown on the product page.
 * "specs" is null for products without a spec table (accessories, peripherals).
 */
public record ProductDetailDto(
        Long id,
        String sku,
        String name,
        String brand,
        String description,
        BigDecimal price,
        boolean inStock,
        Integer stockQuantity,
        ProductType productType,
        CategoryDto category,
        Integer warrantyMonths,
        Integer performanceScore,
        String imageUrl,
        ProductSpecsDto specs) {

    /** Must be called inside a transaction, because product.getCategory() is loaded lazily. */
    public static ProductDetailDto from(Product p, ProductSpecsDto specs) {
        return new ProductDetailDto(
                p.getId(), p.getSku(), p.getName(), p.getBrand(), p.getDescription(), p.getPrice(),
                p.getStockQuantity() > 0, p.getStockQuantity(), p.getProductType(),
                CategoryDto.from(p.getCategory()), p.getWarrantyMonths(), p.getPerformanceScore(),
                p.getImageUrl(), specs);
    }
}
