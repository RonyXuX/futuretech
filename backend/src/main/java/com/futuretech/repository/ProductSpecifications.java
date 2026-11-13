package com.futuretech.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.futuretech.dto.ProductFilter;
import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;

/**
 * Small reusable WHERE conditions for products. Each method returns one condition;
 * fromFilter() joins together only the ones the user actually asked for.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Builds the full condition: always "active", plus each filter that is not null. */
    public static Specification<Product> fromFilter(ProductFilter f) {
        Specification<Product> spec = isActive();
        if (f.search() != null && !f.search().isBlank()) {
            spec = spec.and(nameOrBrandContains(f.search()));
        }
        if (f.categorySlug() != null && !f.categorySlug().isBlank()) {
            spec = spec.and(inCategory(f.categorySlug()));
        }
        if (f.type() != null) {
            spec = spec.and(hasType(f.type()));
        }
        if (f.brand() != null && !f.brand().isBlank()) {
            spec = spec.and(hasBrand(f.brand()));
        }
        if (f.minPrice() != null) {
            spec = spec.and(priceAtLeast(f.minPrice()));
        }
        if (f.maxPrice() != null) {
            spec = spec.and(priceAtMost(f.maxPrice()));
        }
        if (Boolean.TRUE.equals(f.inStock())) {
            spec = spec.and(inStock());
        }
        return spec;
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /** Case-insensitive "contains" on name or brand, e.g. "rtx" matches "GeForce RTX 5070". */
    public static Specification<Product> nameOrBrandContains(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("brand")), pattern, '\\'));
    }

    public static Specification<Product> inCategory(String slug) {
        return (root, query, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    public static Specification<Product> hasType(ProductType type) {
        return (root, query, cb) -> cb.equal(root.get("productType"), type);
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase());
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }

    /** Stops the user's % and _ from acting as SQL wildcards. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
